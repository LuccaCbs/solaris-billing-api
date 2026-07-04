package com.luccavergara.solaris.billing.controller;

import com.luccavergara.solaris.billing.billing.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        try {
            stripeCheckoutService.processWebhookPayload(payload, signature);
        } catch (IllegalArgumentException ex) {
            log.warn("Rejected Stripe webhook: {}", ex.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception ex) {
            log.error("Failed to process Stripe webhook: {}", ex.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
