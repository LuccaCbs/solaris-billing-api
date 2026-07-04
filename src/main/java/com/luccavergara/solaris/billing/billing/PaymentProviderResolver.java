package com.luccavergara.solaris.billing.billing;

import com.luccavergara.solaris.billing.entity.BillingProvider;
import com.luccavergara.solaris.billing.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderResolver {

    public BillingProvider resolve(Organization organization) {
        String countryCode = organization.getCountryCode() != null
                ? organization.getCountryCode().trim().toUpperCase()
                : "";

        if ("AR".equals(countryCode)) {
            return BillingProvider.MERCADOPAGO;
        }

        String currency = organization.getDefaultCurrency() != null
                ? organization.getDefaultCurrency().trim().toUpperCase()
                : "";

        if ("ES".equals(countryCode) || "EUR".equals(currency)) {
            return BillingProvider.STRIPE;
        }

        return BillingProvider.MERCADOPAGO;
    }
}
