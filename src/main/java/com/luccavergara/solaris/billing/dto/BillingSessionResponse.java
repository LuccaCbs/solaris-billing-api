package com.luccavergara.solaris.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BillingSessionResponse {

    private final UUID sessionId;
    private final String email;
    private final LocalDateTime expiresAt;
}
