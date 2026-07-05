package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SubscriptionCheckoutResponse {

    private final String status;
    private final String message;
    private final String checkoutUrl;
    private final String redirectUrl;
    private final Long paymentIntentId;
    private final String provider;
    private final SubscriptionPlanCode planCode;
    private final BigDecimal originalPrice;
    private final BigDecimal finalPrice;
    private final Integer discountPercent;
    private final String currency;
    private final String promoCode;
}
