package com.luccavergara.solaris.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StoreAddonQuoteResponse {

    private final String currency;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal addonSubtotal;
    private final String currentPlanCode;
    private final String currentPlanDisplayName;
    private final BigDecimal currentPlanPrice;
    private final int currentMaxStores;
    private final int currentExtraStoresPurchased;
    private final BigDecimal projectedMonthlyTotal;
    private final String provider;
}
