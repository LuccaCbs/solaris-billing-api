package com.luccavergara.solaris.billing.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MercadoPagoWebhookValidator {

    @Value("${application.billing.mercadopago.webhook-secret:}")
    private String webhookSecret;

    public boolean isValid(String signatureHeader, String requestId, String dataId) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("Mercado Pago webhook secret is not configured; skipping signature validation");
            return true;
        }

        if (!StringUtils.hasText(signatureHeader) || !StringUtils.hasText(requestId) || !StringUtils.hasText(dataId)) {
            return false;
        }

        Map<String, String> parts = Arrays.stream(signatureHeader.split(","))
                .map(String::trim)
                .map(part -> part.split("=", 2))
                .filter(part -> part.length == 2)
                .collect(Collectors.toMap(part -> part[0], part -> part[1], (left, right) -> right));

        String timestamp = parts.get("ts");
        String receivedHash = parts.get("v1");

        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(receivedHash)) {
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        String expectedHash = hmacSha256Hex(webhookSecret, manifest);

        return expectedHash.equalsIgnoreCase(receivedHash);
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not validate Mercado Pago webhook signature");
        }
    }
}
