package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PlanCatalogResponse {

    private final List<PlanResponse> plans;
    private final FreemiumPlanResponse freemiumPlan;
    private final String currency;

    @Getter
    @Builder
    public static class PlanResponse {
        private final SubscriptionPlanCode code;
        private final String displayName;
        private final String description;
        private final String tagline;
        private final boolean recommended;
        private final List<String> features;
        private final Integer maxStores;
        private final BigDecimal price;
        private final String currency;
    }

    @Getter
    @Builder
    public static class FreemiumPlanResponse {
        private final SubscriptionPlanCode code;
        private final String displayName;
        private final String tagline;
        private final List<String> features;
    }
}
