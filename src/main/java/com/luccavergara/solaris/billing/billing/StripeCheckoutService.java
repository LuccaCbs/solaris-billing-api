package com.luccavergara.solaris.billing.billing;

import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.BillingPortalPaymentIntentRepository;
import com.luccavergara.solaris.billing.service.BillingPromoCodeService;
import com.luccavergara.solaris.billing.service.EmailService;
import com.luccavergara.solaris.billing.service.PaymentFulfillmentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCheckoutService {

    private static final String EXTERNAL_REFERENCE_PREFIX = "solaris:portal-intent:";
    private static final String SESSION_METADATA_KEY = "payment_intent_id";

    private final BillingPortalPaymentIntentRepository paymentIntentRepository;
    private final PaymentFulfillmentService paymentFulfillmentService;
    private final EmailService emailService;

    @Value("${application.portal.url:http://localhost:8081}")
    private String portalUrl;

    @Value("${application.app.url:https://www.solarismanager.com}")
    private String appUrl;

    @Value("${application.billing.stripe.secret-key:}")
    private String secretKey;

    @Value("${application.billing.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${application.billing.store-addon-price-eur:15}")
    private BigDecimal storeAddonPriceEur;

    public boolean isConfigured() {
        return StringUtils.hasText(secretKey);
    }

    @Transactional
    public String createSubscriptionCheckoutSession(
            BillingPortalPaymentIntent paymentIntent,
            Organization organization,
            BillingPromoCodeService.PromoQuote quote
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        Stripe.apiKey = secretKey;

        String successUrl = normalizeBaseUrl(appUrl) + "/admin/billing?status=success";
        String cancelUrl = normalizeBaseUrl(portalUrl) + "/?status=failure";

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata(SESSION_METADATA_KEY, paymentIntent.getId().toString())
                    .putMetadata("organization_id", organization.getId().toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(quote.currency().toLowerCase())
                                                    .setUnitAmount(toMinorUnits(quote.finalPrice()))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Solaris - Plan " + quote.effectivePlanCode().name())
                                                                    .setDescription(
                                                                            "Subscription plan " + quote.effectivePlanCode().name()
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException ex) {
            log.error("Failed to create Stripe subscription checkout session: {}", ex.getMessage());
            throw new IllegalStateException("Could not create Stripe checkout session");
        }
    }

    @Transactional
    public String createCheckoutSession(
            BillingPortalPaymentIntent paymentIntent,
            Organization organization,
            int quantity
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        Stripe.apiKey = secretKey;

        String returnBase = normalizeBaseUrl(portalUrl) + "/?status=";

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(returnBase + "success")
                    .setCancelUrl(returnBase + "failure")
                    .putMetadata(SESSION_METADATA_KEY, paymentIntent.getId().toString())
                    .putMetadata("organization_id", organization.getId().toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) quantity)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(toMinorUnits(storeAddonPriceEur))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Solaris - Additional store")
                                                                    .setDescription(
                                                                            "Purchase of " + quantity + " additional store slot(s)"
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException ex) {
            log.error("Failed to create Stripe checkout session: {}", ex.getMessage());
            throw new IllegalStateException("Could not create Stripe checkout session");
        }
    }

    @Transactional
    public void processWebhookPayload(String payload, String signatureHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElse(null);

        if (session == null) {
            log.warn("Stripe webhook checkout.session.completed without session payload");
            return;
        }

        fulfillCheckoutSession(session);
    }

    @Transactional
    public void fulfillCheckoutSession(Session session) {
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        BillingPortalPaymentIntent paymentIntent = resolvePaymentIntent(session)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found for Stripe session"));

        if (paymentIntent.getStatus() == BillingPortalPaymentIntentStatus.PAID) {
            return;
        }

        paymentFulfillmentService.fulfillPaidIntent(paymentIntent, BillingProvider.STRIPE);

        String recipientEmail = paymentIntent.getSession().getEmail();
        if (StringUtils.hasText(recipientEmail)) {
            emailService.sendPurchaseConfirmation(
                    recipientEmail,
                    paymentIntent.getOrganization().getDisplayName() != null
                            ? paymentIntent.getOrganization().getDisplayName()
                            : paymentIntent.getOrganization().getRazonSocial(),
                    paymentIntent.getQuantity(),
                    buildActivationCode(paymentIntent.getId())
            );
        }
    }

    private java.util.Optional<BillingPortalPaymentIntent> resolvePaymentIntent(Session session) {
        if (session.getMetadata() != null && StringUtils.hasText(session.getMetadata().get(SESSION_METADATA_KEY))) {
            try {
                Long paymentIntentId = Long.parseLong(session.getMetadata().get(SESSION_METADATA_KEY));
                return paymentIntentRepository.findById(paymentIntentId);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        return java.util.Optional.empty();
    }

    private String buildActivationCode(Long paymentIntentId) {
        return "SOL-" + paymentIntentId;
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
