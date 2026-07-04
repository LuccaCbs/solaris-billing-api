package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.validation.ValidBillingEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank
    @ValidBillingEmail
    private String email;
}
