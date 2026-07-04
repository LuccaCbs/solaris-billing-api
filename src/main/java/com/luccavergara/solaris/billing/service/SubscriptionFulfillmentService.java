package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.OrganizationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionFulfillmentService {

    private static final int TRIAL_DAYS = 14;

    private final OrganizationSubscriptionRepository subscriptionRepository;

    @Transactional
    public OrganizationSubscription ensureSubscription(Organization organization) {
        return subscriptionRepository.findByOrganization(organization)
                .orElseGet(() -> createDefaultSubscription(organization));
    }

    @Transactional
    public void applyStoreAddonPurchase(Long organizationId, int quantity) {
        OrganizationSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for organization"));

        if (!subscription.isBillingActive()) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscription.setExtraStoresPurchased(subscription.getExtraStoresPurchased() + quantity);
        subscription.setBillingProvider(BillingProvider.MERCADOPAGO);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    private OrganizationSubscription createDefaultSubscription(Organization organization) {
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.save(
                OrganizationSubscription.builder()
                        .organization(organization)
                        .planCode(SubscriptionPlanCode.POS)
                        .status(SubscriptionStatus.TRIALING)
                        .maxStores(1)
                        .extraStoresPurchased(0)
                        .billingProvider(BillingProvider.NONE)
                        .trialEndsAt(now.plusDays(TRIAL_DAYS))
                        .currentPeriodStart(now)
                        .currentPeriodEnd(now.plusDays(TRIAL_DAYS))
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );
    }
}
