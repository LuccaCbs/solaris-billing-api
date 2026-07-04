package com.luccavergara.solaris.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StoreAddonCheckoutResponse {

    private final String status;
    private final String message;
    private final String checkoutUrl;
    private final Long paymentIntentId;
    private final String provider;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final String currency;
}
