package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.validation.ValidBillingEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmEmailRequest {

    @NotBlank
    @ValidBillingEmail
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits")
    private String otp;
}
