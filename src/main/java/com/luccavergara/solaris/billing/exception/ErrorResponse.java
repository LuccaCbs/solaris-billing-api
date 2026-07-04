package com.luccavergara.solaris.billing.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
}
