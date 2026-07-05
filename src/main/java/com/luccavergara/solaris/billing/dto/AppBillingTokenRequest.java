package com.luccavergara.solaris.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppBillingTokenRequest {

    @NotBlank
    private String billingToken;
}
