package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.billing.MercadoPagoClient;
import com.luccavergara.solaris.billing.dto.StoreAddonCheckoutResponse;
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
public class StoreAddonCheckoutService {

    private static final String EXTERNAL_REFERENCE_PREFIX = "solaris:portal-intent:";
    private static final List<OrganizationMemberRole> BILLABLE_ROLES = List.of(
            OrganizationMemberRole.OWNER,
            OrganizationMemberRole.ADMIN
    );

    private final BillingPortalSessionRepository sessionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final BillingPortalPaymentIntentRepository paymentIntentRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final SubscriptionFulfillmentService subscriptionFulfillmentService;

    @Value("${application.portal.url:http://localhost:8081}")
    private String portalUrl;

    @Value("${application.billing.api-public-url:}")
    private String apiPublicUrl;

    @Value("${application.billing.mercadopago.use-sandbox:false}")
    private boolean useSandbox;

    @Value("${application.billing.store-addon-price-ars:15000}")
    private BigDecimal storeAddonPriceArs;

    @Transactional
    public StoreAddonCheckoutResponse createCheckout(UUID sessionId, Long organizationId, int quantity) {
        if (!mercadoPagoClient.isConfigured()) {
            throw new IllegalStateException("Mercado Pago is not configured");
        }

        BillingPortalSession session = sessionRepository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired billing session"));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        assertBillableMembership(session.getUser(), organization);

        if (!isMercadoPagoOrganization(organization)) {
            throw new IllegalStateException("Mercado Pago checkout is only available for AR organizations");
        }

        subscriptionFulfillmentService.ensureSubscription(organization);

        BillingPortalPaymentIntent paymentIntent = paymentIntentRepository.save(
                BillingPortalPaymentIntent.builder()
                        .session(session)
                        .organization(organization)
                        .productCode("STORE_ADDON")
                        .quantity(quantity)
                        .provider("MERCADOPAGO")
                        .status(BillingPortalPaymentIntentStatus.PENDING)
                        .build()
        );

        String externalReference = EXTERNAL_REFERENCE_PREFIX + paymentIntent.getId();
        paymentIntent.setExternalReference(externalReference);
        paymentIntentRepository.save(paymentIntent);

        String returnBase = normalizeBaseUrl(portalUrl) + "/?status=";
        MercadoPagoClient.MercadoPagoPreference preference = mercadoPagoClient.createStoreAddonPreference(
                new MercadoPagoClient.CreatePreferenceCommand(
                        "Solaris - Sucursal adicional",
                        "Compra de " + quantity + " sucursal(es) adicional(es)",
                        quantity,
                        storeAddonPriceArs,
                        "ARS",
                        externalReference,
                        resolveNotificationUrl(),
                        returnBase + "success",
                        returnBase + "failure",
                        returnBase + "pending"
                )
        );

        paymentIntent.setStatus(BillingPortalPaymentIntentStatus.CHECKOUT_STARTED);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(paymentIntent);

        String checkoutUrl = useSandbox ? preference.sandboxInitPoint() : preference.initPoint();

        return StoreAddonCheckoutResponse.builder()
                .status("READY")
                .message("Redirect to Mercado Pago to complete payment")
                .checkoutUrl(checkoutUrl)
                .paymentIntentId(paymentIntent.getId())
                .provider("MERCADOPAGO")
                .quantity(quantity)
                .unitPrice(storeAddonPriceArs)
                .currency("ARS")
                .build();
    }

    @Transactional
    public void processPaymentNotification(String paymentId) {
        MercadoPagoClient.MercadoPagoPayment payment = mercadoPagoClient.getPayment(paymentId);

        if (!payment.isApproved()) {
            return;
        }

        BillingPortalPaymentIntent paymentIntent = resolvePaymentIntent(payment)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));

        if (paymentIntent.getStatus() == BillingPortalPaymentIntentStatus.PAID) {
            return;
        }

        paymentIntent.setStatus(BillingPortalPaymentIntentStatus.PAID);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(paymentIntent);

        subscriptionFulfillmentService.applyStoreAddonPurchase(
                paymentIntent.getOrganization().getId(),
                paymentIntent.getQuantity()
        );
    }

    private java.util.Optional<BillingPortalPaymentIntent> resolvePaymentIntent(MercadoPagoClient.MercadoPagoPayment payment) {
        if (StringUtils.hasText(payment.externalReference())) {
            java.util.Optional<BillingPortalPaymentIntent> byReference = paymentIntentRepository
                    .findByExternalReference(payment.externalReference());

            if (byReference.isPresent()) {
                return byReference;
            }
        }

        Long paymentIntentId = parsePaymentIntentId(payment.externalReference());

        if (paymentIntentId == null) {
            return java.util.Optional.empty();
        }

        return paymentIntentRepository.findById(paymentIntentId);
    }

    private Long parsePaymentIntentId(String externalReference) {
        if (!StringUtils.hasText(externalReference) || !externalReference.startsWith(EXTERNAL_REFERENCE_PREFIX)) {
            return null;
        }

        try {
            return Long.parseLong(externalReference.substring(EXTERNAL_REFERENCE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private boolean isMercadoPagoOrganization(Organization organization) {
        String countryCode = organization.getCountryCode();
        return countryCode != null && "AR".equalsIgnoreCase(countryCode.trim());
    }

    private String resolveNotificationUrl() {
        if (!StringUtils.hasText(apiPublicUrl)) {
            throw new IllegalStateException("application.billing.api-public-url is required for Mercado Pago webhooks");
        }

        return normalizeBaseUrl(apiPublicUrl) + "/api/v1/public/webhooks/mercadopago";
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
