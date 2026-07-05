package com.luccavergara.solaris.billing.billing;

import com.luccavergara.solaris.billing.entity.BillingProvider;
import com.luccavergara.solaris.billing.entity.Organization;
import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingPricingService {

    @Value("${application.billing.plan-business-price-ars:50000}")
    private BigDecimal businessPriceArs;

    @Value("${application.billing.plan-business-price-eur:49}")
    private BigDecimal businessPriceEur;

    @Value("${application.billing.plan-scale-price-ars:80000}")
    private BigDecimal scalePriceArs;

    @Value("${application.billing.plan-scale-price-eur:79}")
    private BigDecimal scalePriceEur;

    @Value("${application.billing.store-addon-price-ars:15000}")
    private BigDecimal storeAddonPriceArs;

    @Value("${application.billing.store-addon-price-eur:15}")
    private BigDecimal storeAddonPriceEur;

    public String resolveCurrency(Organization organization) {
        if (organization.getDefaultCurrency() != null && !organization.getDefaultCurrency().isBlank()) {
            return organization.getDefaultCurrency().toUpperCase();
        }

        if ("ES".equalsIgnoreCase(organization.getCountryCode())) {
            return "EUR";
        }

        return "ARS";
    }

    public BillingProvider resolvePreferredProvider(Organization organization) {
        String currency = resolveCurrency(organization);

        if ("EUR".equals(currency) || "ES".equalsIgnoreCase(organization.getCountryCode())) {
            return BillingProvider.STRIPE;
        }

        return BillingProvider.MERCADOPAGO;
    }

    public BigDecimal getPlanPrice(Organization organization, SubscriptionPlanCode planCode) {
        String currency = resolveCurrency(organization);

        return switch (planCode) {
            case BUSINESS -> "EUR".equals(currency) ? businessPriceEur : businessPriceArs;
            case SCALE -> "EUR".equals(currency) ? scalePriceEur : scalePriceArs;
            default -> BigDecimal.ZERO;
        };
    }

    public BigDecimal getStoreAddonPrice(Organization organization) {
        return "EUR".equals(resolveCurrency(organization)) ? storeAddonPriceEur : storeAddonPriceArs;
    }
}
