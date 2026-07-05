package com.luccavergara.solaris.billing.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppBillingPrefillResponse {

    private final String email;
    private final Long preferredOrganizationId;
}
