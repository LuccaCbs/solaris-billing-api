package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.billing.BillingPricingService;
import com.luccavergara.solaris.billing.billing.MercadoPagoClient;
import com.luccavergara.solaris.billing.billing.PaymentProviderResolver;
import com.luccavergara.solaris.billing.billing.StripeCheckoutService;
import com.luccavergara.solaris.billing.dto.SubscriptionCheckoutResponse;
import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionCheckoutService {

    private static final String PRODUCT_CODE = "SUBSCRIPTION";
    private static final String EXTERNAL_REFERENCE_PREFIX = "solaris:portal-intent:";

    private static final List<OrganizationMemberRole> BILLABLE_ROLES = List.of(
            OrganizationMemberRole.OWNER,
            OrganizationMemberRole.ADMIN
    );

    private final BillingPortalSessionRepository sessionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final BillingPortalPaymentIntentRepository paymentIntentRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final StripeCheckoutService stripeCheckoutService;
    private final PaymentProviderResolver paymentProviderResolver;
    private final BillingPricingService billingPricingService;
    private final BillingPromoCodeService billingPromoCodeService;
    private final SubscriptionFulfillmentService subscriptionFulfillmentService;
    private final EmailService emailService;

    @Value("${application.portal.url:http://localhost:8081}")
    private String portalUrl;

    @Value("${application.app.url:https://www.solarismanager.com}")
    private String appUrl;

    @Value("${application.billing.api-public-url:}")
    private String apiPublicUrl;

    @Value("${application.billing.mercadopago.use-sandbox:false}")
    private boolean useSandbox;

    @Transactional
    public SubscriptionCheckoutResponse createCheckout(
            UUID sessionId,
            Long organizationId,
            SubscriptionPlanCode planCode,
            String promoCode
    ) {
        BillingPortalSession session = sessionRepository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired billing session"));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        subscriptionPlanRepository.findById(planCode)
                .filter(SubscriptionPlan::isPublic)
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Plan is not available for purchase"));

        assertBillableMembership(session.getUser(), organization);
        subscriptionFulfillmentService.ensureSubscription(organization);

        BillingPromoCodeService.PromoQuote quote = billingPromoCodeService.resolveQuote(
                organization,
                planCode,
                promoCode,
                session.getUser().getId()
        );

        if (quote.finalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return fulfillFreeCheckout(session, organization, quote);
        }

        BillingProvider provider = paymentProviderResolver.resolve(organization);

        BillingPortalPaymentIntent paymentIntent = paymentIntentRepository.save(
                BillingPortalPaymentIntent.builder()
                        .session(session)
                        .organization(organization)
                        .productCode(PRODUCT_CODE)
                        .planCode(quote.effectivePlanCode())
                        .promoCodeId(quote.promoCode() != null ? quote.promoCode().getId() : null)
                        .finalAmount(quote.finalPrice())
                        .quantity(1)
                        .provider(provider.name())
                        .status(BillingPortalPaymentIntentStatus.PENDING)
                        .build()
        );

        String externalReference = EXTERNAL_REFERENCE_PREFIX + paymentIntent.getId();
        paymentIntent.setExternalReference(externalReference);
        paymentIntentRepository.save(paymentIntent);

        if (provider == BillingProvider.STRIPE) {
            return createStripeCheckout(paymentIntent, organization, quote);
        }

        return createMercadoPagoCheckout(paymentIntent, organization, quote);
    }

    private SubscriptionCheckoutResponse fulfillFreeCheckout(
            BillingPortalSession session,
            Organization organization,
            BillingPromoCodeService.PromoQuote quote
    ) {
        BillingPortalPaymentIntent paymentIntent = paymentIntentRepository.save(
                BillingPortalPaymentIntent.builder()
                        .session(session)
                        .organization(organization)
                        .productCode(PRODUCT_CODE)
                        .planCode(quote.effectivePlanCode())
                        .promoCodeId(quote.promoCode() != null ? quote.promoCode().getId() : null)
                        .finalAmount(BigDecimal.ZERO)
                        .quantity(1)
                        .provider(BillingProvider.NONE.name())
                        .status(BillingPortalPaymentIntentStatus.PAID)
                        .build()
        );

        Integer durationDays = quote.promoCode() != null ? quote.promoCode().getDurationDays() : null;

        if (quote.promoCode() != null) {
            billingPromoCodeService.redeemPromoForCheckout(
                    quote.promoCode(),
                    organization,
                    session.getUser().getId(),
                    quote.effectivePlanCode()
            );
        }

        subscriptionFulfillmentService.applySubscriptionPurchase(
                organization.getId(),
                quote.effectivePlanCode(),
                BillingProvider.NONE,
                durationDays
        );

        sendConfirmationEmail(session, organization, paymentIntent.getId());

        return SubscriptionCheckoutResponse.builder()
                .status("FULFILLED")
                .message("Plan activated successfully")
                .redirectUrl(buildAppSuccessUrl())
                .paymentIntentId(paymentIntent.getId())
                .provider(BillingProvider.NONE.name())
                .planCode(quote.effectivePlanCode())
                .originalPrice(quote.originalPrice())
                .finalPrice(BigDecimal.ZERO)
                .discountPercent(quote.discountPercent())
                .currency(quote.currency())
                .promoCode(quote.promoCode() != null ? quote.promoCode().getCode() : null)
                .build();
    }

    private SubscriptionCheckoutResponse createMercadoPagoCheckout(
            BillingPortalPaymentIntent paymentIntent,
            Organization organization,
            BillingPromoCodeService.PromoQuote quote
    ) {
        if (!mercadoPagoClient.isConfigured()) {
            throw new IllegalStateException("Mercado Pago is not configured");
        }

        String returnBase = buildAppSuccessUrl() + (buildAppSuccessUrl().contains("?") ? "&" : "?") + "status=";
        MercadoPagoClient.MercadoPagoPreference preference = mercadoPagoClient.createStoreAddonPreference(
                new MercadoPagoClient.CreatePreferenceCommand(
                        "Solaris - Plan " + quote.effectivePlanCode().name(),
                        "Suscripción plan " + quote.effectivePlanCode().name(),
                        1,
                        quote.finalPrice(),
                        quote.currency(),
                        paymentIntent.getExternalReference(),
                        resolveMercadoPagoNotificationUrl(),
                        returnBase + "success",
                        returnBase + "failure",
                        returnBase + "pending"
                )
        );

        paymentIntent.setStatus(BillingPortalPaymentIntentStatus.CHECKOUT_STARTED);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(paymentIntent);

        return SubscriptionCheckoutResponse.builder()
                .status("READY")
                .message("Redirect to Mercado Pago to complete payment")
                .checkoutUrl(useSandbox ? preference.sandboxInitPoint() : preference.initPoint())
                .paymentIntentId(paymentIntent.getId())
                .provider(BillingProvider.MERCADOPAGO.name())
                .planCode(quote.effectivePlanCode())
                .originalPrice(quote.originalPrice())
                .finalPrice(quote.finalPrice())
                .discountPercent(quote.discountPercent())
                .currency(quote.currency())
                .promoCode(quote.promoCode() != null ? quote.promoCode().getCode() : null)
                .build();
    }

    private SubscriptionCheckoutResponse createStripeCheckout(
            BillingPortalPaymentIntent paymentIntent,
            Organization organization,
            BillingPromoCodeService.PromoQuote quote
    ) {
        String checkoutUrl = stripeCheckoutService.createSubscriptionCheckoutSession(
                paymentIntent,
                organization,
                quote
        );

        paymentIntent.setStatus(BillingPortalPaymentIntentStatus.CHECKOUT_STARTED);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(paymentIntent);

        return SubscriptionCheckoutResponse.builder()
                .status("READY")
                .message("Redirect to Stripe to complete payment")
                .checkoutUrl(checkoutUrl)
                .paymentIntentId(paymentIntent.getId())
                .provider(BillingProvider.STRIPE.name())
                .planCode(quote.effectivePlanCode())
                .originalPrice(quote.originalPrice())
                .finalPrice(quote.finalPrice())
                .discountPercent(quote.discountPercent())
                .currency(quote.currency())
                .promoCode(quote.promoCode() != null ? quote.promoCode().getCode() : null)
                .build();
    }

    private void sendConfirmationEmail(BillingPortalSession session, Organization organization, Long paymentIntentId) {
        if (!StringUtils.hasText(session.getEmail())) {
            return;
        }

        String orgName = organization.getDisplayName() != null
                ? organization.getDisplayName()
                : organization.getRazonSocial();

        emailService.sendPurchaseConfirmation(session.getEmail(), orgName, 1, "SOL-" + paymentIntentId);
    }

    private void assertBillableMembership(SolarisUser user, Organization organization) {
        List<OrganizationMember> memberships = organizationMemberRepository.findBillableMemberships(
                user,
                OrganizationMemberStatus.ACTIVE,
                BILLABLE_ROLES
        );

        boolean allowed = memberships.stream()
                .anyMatch(member -> member.getOrganization().getId().equals(organization.getId()));

        if (!allowed) {
            throw new IllegalStateException("You are not authorized to manage billing for this organization");
        }
    }

    private String resolveMercadoPagoNotificationUrl() {
        if (!StringUtils.hasText(apiPublicUrl)) {
            throw new IllegalStateException("application.billing.api-public-url is required for Mercado Pago webhooks");
        }

        return normalizeBaseUrl(apiPublicUrl) + "/api/v1/public/webhooks/mercadopago";
    }

    private String buildAppSuccessUrl() {
        return normalizeBaseUrl(appUrl) + "/admin/billing";
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
