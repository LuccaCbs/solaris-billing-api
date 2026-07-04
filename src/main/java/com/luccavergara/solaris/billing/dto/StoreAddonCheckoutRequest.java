package com.luccavergara.solaris.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StoreAddonCheckoutRequest {

    @NotNull
    private UUID sessionId;

    @NotNull
    private Long organizationId;

    @Min(1)
    @Max(10)
    private Integer quantity = 1;
}
