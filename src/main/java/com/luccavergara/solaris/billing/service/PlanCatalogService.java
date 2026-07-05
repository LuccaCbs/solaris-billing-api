package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.billing.BillingPricingService;
import com.luccavergara.solaris.billing.dto.PlanCatalogResponse;
import com.luccavergara.solaris.billing.entity.Organization;
import com.luccavergara.solaris.billing.entity.SubscriptionPlan;
import com.luccavergara.solaris.billing.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanCatalogService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingPricingService billingPricingService;

    @Transactional(readOnly = true)
    public PlanCatalogResponse getPublicPlans(Organization organization) {
        String currency = billingPricingService.resolveCurrency(organization);

        List<PlanCatalogResponse.PlanResponse> plans = subscriptionPlanRepository
                .findByIsPublicTrueAndActiveTrueOrderBySortOrderAsc()
                .stream()
                .filter(plan -> plan.getCode() != com.luccavergara.solaris.billing.entity.SubscriptionPlanCode.POS)
                .map(plan -> toPlanResponse(plan, organization, currency))
                .toList();

        return PlanCatalogResponse.builder()
                .plans(plans)
                .currency(currency)
                .build();
    }

    private PlanCatalogResponse.PlanResponse toPlanResponse(
            SubscriptionPlan plan,
            Organization organization,
            String currency
    ) {
        return PlanCatalogResponse.PlanResponse.builder()
                .code(plan.getCode())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .maxStores(plan.getMaxStores())
                .price(billingPricingService.getPlanPrice(organization, plan.getCode()))
                .currency(currency)
                .build();
    }
}
