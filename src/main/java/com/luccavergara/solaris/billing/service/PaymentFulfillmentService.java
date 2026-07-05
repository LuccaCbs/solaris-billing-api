package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.BillingPortalPaymentIntentRepository;
import com.luccavergara.solaris.billing.repository.OrganizationSubscriptionRepository;
import com.luccavergara.solaris.billing.repository.PromoCodeRepository;
import com.luccavergara.solaris.billing.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentFulfillmentService {

    private static final String PRODUCT_SUBSCRIPTION = "SUBSCRIPTION";

    private final SubscriptionFulfillmentService subscriptionFulfillmentService;
    private final BillingPromoCodeService billingPromoCodeService;
    private final PromoCodeRepository promoCodeRepository;
    private final BillingPortalPaymentIntentRepository paymentIntentRepository;

    @Transactional
    public void fulfillPaidIntent(BillingPortalPaymentIntent paymentIntent, BillingProvider billingProvider) {
        if (paymentIntent.getStatus() == BillingPortalPaymentIntentStatus.PAID) {
            return;
        }

        paymentIntent.setStatus(BillingPortalPaymentIntentStatus.PAID);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(paymentIntent);

        if (PRODUCT_SUBSCRIPTION.equals(paymentIntent.getProductCode())) {
            fulfillSubscription(paymentIntent, billingProvider);
            return;
        }

        subscriptionFulfillmentService.applyStoreAddonPurchase(
                paymentIntent.getOrganization().getId(),
                paymentIntent.getQuantity(),
                billingProvider
        );
    }

    private void fulfillSubscription(BillingPortalPaymentIntent paymentIntent, BillingProvider billingProvider) {
        SubscriptionPlanCode planCode = paymentIntent.getPlanCode();
        if (planCode == null) {
            throw new IllegalStateException("Subscription payment intent is missing plan code");
        }

        Integer durationDays = null;

        if (paymentIntent.getPromoCodeId() != null) {
            PromoCode promoCode = promoCodeRepository.findById(paymentIntent.getPromoCodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

            durationDays = promoCode.getDurationDays();

            billingPromoCodeService.redeemPromoForCheckout(
                    promoCode,
                    paymentIntent.getOrganization(),
                    paymentIntent.getSession().getUser().getId(),
                    planCode
            );
        }

        subscriptionFulfillmentService.applySubscriptionPurchase(
                paymentIntent.getOrganization().getId(),
                planCode,
                billingProvider,
                durationDays
        );
    }
}
