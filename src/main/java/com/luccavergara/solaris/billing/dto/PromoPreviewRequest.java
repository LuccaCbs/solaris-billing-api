package com.luccavergara.solaris.billing.dto;

import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PromoPreviewRequest {

    @NotNull
    private UUID sessionId;

    @NotNull
    private Long organizationId;

    @NotNull
    private SubscriptionPlanCode planCode;

    @NotNull
    private String promoCode;
}
