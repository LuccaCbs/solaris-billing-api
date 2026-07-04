package com.luccavergara.solaris.billing.controller;

import com.luccavergara.solaris.billing.billing.MercadoPagoWebhookValidator;
import com.luccavergara.solaris.billing.service.StoreAddonCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/webhooks/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final StoreAddonCheckoutService storeAddonCheckoutService;
    private final MercadoPagoWebhookValidator webhookValidator;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String queryId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dottedDataId,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId
    ) {
        String resolvedTopic = firstNonBlank(topic, type, extractNested(body, "type"), extractNested(body, "action"));
        String paymentId = firstNonBlank(queryId, dottedDataId, extractNested(body, "data.id"), extractDataId(body));

        if (!isPaymentNotification(resolvedTopic) || !StringUtils.hasText(paymentId)) {
            return ResponseEntity.ok().build();
        }

        if (!webhookValidator.isValid(signature, requestId, paymentId)) {
            log.warn("Rejected Mercado Pago webhook with invalid signature for payment {}", paymentId);
            return ResponseEntity.status(401).build();
        }

        try {
            storeAddonCheckoutService.processPaymentNotification(paymentId);
        } catch (Exception ex) {
            log.error("Failed to process Mercado Pago webhook for payment {}: {}", paymentId, ex.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Void> handleWebhookGet(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id
    ) {
        if (!isPaymentNotification(topic) || !StringUtils.hasText(id)) {
            return ResponseEntity.ok().build();
        }

        try {
            storeAddonCheckoutService.processPaymentNotification(id);
        } catch (Exception ex) {
            log.error("Failed to process Mercado Pago GET webhook for payment {}: {}", id, ex.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private boolean isPaymentNotification(String topic) {
        return StringUtils.hasText(topic) && topic.toLowerCase().contains("payment");
    }

    private String extractDataId(Map<String, Object> body) {
        if (body == null || !(body.get("data") instanceof Map<?, ?> data)) {
            return null;
        }

        Object id = data.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    private String extractNested(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }

        return String.valueOf(body.get(key));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }
}
