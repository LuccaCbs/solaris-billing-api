package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PromoPreviewResponse {

    private final boolean valid;
    private final String message;
    private final SubscriptionPlanCode planCode;
    private final SubscriptionPlanCode effectivePlanCode;
    private final BigDecimal originalPrice;
    private final BigDecimal finalPrice;
    private final Integer discountPercent;
    private final String currency;
    private final boolean requiresPayment;
    private final String promoCode;
}
