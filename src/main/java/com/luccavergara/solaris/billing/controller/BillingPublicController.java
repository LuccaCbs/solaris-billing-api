package com.luccavergara.solaris.billing.controller;

import com.luccavergara.solaris.billing.dto.*;
import com.luccavergara.solaris.billing.service.BillingPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class BillingPublicController {

    private final BillingPortalService billingPortalService;

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return billingPortalService.requestEmailVerification(request.getEmail());
    }

    @PostMapping("/confirm-email")
    public BillingSessionResponse confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        return billingPortalService.confirmEmail(request.getEmail(), request.getOtp());
    }

    @GetMapping("/organizations")
    public List<BillingOrganizationResponse> listOrganizations(@RequestParam UUID sessionId) {
        return billingPortalService.listOrganizations(sessionId);
    }
}
