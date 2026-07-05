package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SubscriptionCheckoutRequest {

    @NotNull
    private UUID sessionId;

    @NotNull
    private Long organizationId;

    @NotNull
    private SubscriptionPlanCode planCode;

    private String promoCode;
}
