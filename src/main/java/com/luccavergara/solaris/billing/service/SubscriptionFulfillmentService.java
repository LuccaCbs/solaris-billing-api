package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.OrganizationSubscriptionRepository;
import com.luccavergara.solaris.billing.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionFulfillmentService {

    private static final int TRIAL_DAYS = 14;
    private static final int DEFAULT_PAID_PERIOD_DAYS = 30;

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional
    public OrganizationSubscription ensureSubscription(Organization organization) {
        return subscriptionRepository.findByOrganization(organization)
                .orElseGet(() -> createDefaultSubscription(organization));
    }

    @Transactional
    public void applyStoreAddonPurchase(Long organizationId, int quantity) {
        applyStoreAddonPurchase(organizationId, quantity, BillingProvider.MERCADOPAGO);
    }

    @Transactional
    public void applyStoreAddonPurchase(Long organizationId, int quantity, BillingProvider billingProvider) {
        OrganizationSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for organization"));

        if (!subscription.isBillingActive()) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscription.setExtraStoresPurchased(subscription.getExtraStoresPurchased() + quantity);
        subscription.setBillingProvider(billingProvider);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void applySubscriptionPurchase(
            Long organizationId,
            SubscriptionPlanCode planCode,
            BillingProvider billingProvider,
            Integer durationDays
    ) {
        OrganizationSubscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for organization"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = durationDays != null
                ? now.plusDays(durationDays)
                : now.plusDays(DEFAULT_PAID_PERIOD_DAYS);

        subscription.setPlanCode(planCode);
        subscription.setMaxStores(plan.getMaxStores());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setBillingProvider(billingProvider);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setTrialEndsAt(null);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    private OrganizationSubscription createDefaultSubscription(Organization organization) {
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.save(
                OrganizationSubscription.builder()
                        .organization(organization)
                        .planCode(SubscriptionPlanCode.POS)
                        .status(SubscriptionStatus.PENDING_PLAN)
                        .maxStores(1)
                        .extraStoresPurchased(0)
                        .billingProvider(BillingProvider.NONE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );
    }
}
