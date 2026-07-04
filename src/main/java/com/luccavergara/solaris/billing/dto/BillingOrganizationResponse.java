package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.OrganizationMemberRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillingOrganizationResponse {

    private final Long id;
    private final String name;
    private final String displayName;
    private final String countryCode;
    private final String currency;
    private final OrganizationMemberRole role;
}
