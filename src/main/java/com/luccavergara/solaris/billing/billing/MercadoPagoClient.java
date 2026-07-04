package com.luccavergara.solaris.billing.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoClient {

    private static final String DEFAULT_API_BASE_URL = "https://api.mercadopago.com";

    private final ObjectMapper objectMapper;

    @Value("${application.billing.mercadopago.access-token:}")
    private String accessToken;

    @Value("${application.billing.mercadopago.api-base-url:" + DEFAULT_API_BASE_URL + "}")
    private String apiBaseUrl;

    public boolean isConfigured() {
        return StringUtils.hasText(accessToken);
    }

    public MercadoPagoPreference createStoreAddonPreference(CreatePreferenceCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("items", List.of(Map.of(
                "title", command.itemTitle(),
                "description", command.itemDescription(),
                "quantity", command.quantity(),
                "unit_price", command.unitPrice(),
                "currency_id", command.currency()
        )));

        payload.put("external_reference", command.externalReference());
        payload.put("notification_url", command.notificationUrl());
        payload.put("auto_return", "approved");
        payload.put("statement_descriptor", "SOLARIS");
        payload.put("back_urls", Map.of(
                "success", command.successUrl(),
                "failure", command.failureUrl(),
                "pending", command.pendingUrl()
        ));
        payload.put("payment_methods", Map.of(
                "excluded_payment_types", List.of(
                        Map.of("id", "ticket"),
                        Map.of("id", "atm"),
                        Map.of("id", "bank_transfer"),
                        Map.of("id", "digital_currency")
                ),
                "installments", 1
        ));

        try {
            String responseBody = restClient().post()
                    .uri("/checkout/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String preferenceId = root.path("id").asText(null);
            String initPoint = root.path("init_point").asText(null);
            String sandboxInitPoint = root.path("sandbox_init_point").asText(null);

            if (!StringUtils.hasText(preferenceId) || !StringUtils.hasText(initPoint)) {
                throw new IllegalStateException("Mercado Pago preference response is missing required fields");
            }

            return new MercadoPagoPreference(
                    preferenceId,
                    initPoint,
                    StringUtils.hasText(sandboxInitPoint) ? sandboxInitPoint : initPoint
            );
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago preference HTTP {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Could not create Mercado Pago checkout preference");
        } catch (Exception ex) {
            log.error("Mercado Pago preference error: {}", ex.getMessage());
            throw new IllegalStateException("Could not create Mercado Pago checkout preference");
        }
    }

    public MercadoPagoPayment getPayment(String paymentId) {
        try {
            String responseBody = restClient().get()
                    .uri("/v1/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);

            return new MercadoPagoPayment(
                    root.path("id").asText(null),
                    root.path("status").asText(null),
                    root.path("external_reference").asText(null),
                    root.path("preference_id").asText(null)
            );
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago payment HTTP {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Could not fetch Mercado Pago payment");
        } catch (Exception ex) {
            log.error("Mercado Pago payment error: {}", ex.getMessage());
            throw new IllegalStateException("Could not fetch Mercado Pago payment");
        }
    }

    private RestClient restClient() {
        if (!isConfigured()) {
            throw new IllegalStateException("Mercado Pago access token is not configured");
        }

        return RestClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public record CreatePreferenceCommand(
            String itemTitle,
            String itemDescription,
            int quantity,
            BigDecimal unitPrice,
            String currency,
            String externalReference,
            String notificationUrl,
            String successUrl,
            String failureUrl,
            String pendingUrl
    ) {
    }

    public record MercadoPagoPreference(String id, String initPoint, String sandboxInitPoint) {
    }

    public record MercadoPagoPayment(String id, String status, String externalReference, String preferenceId) {
        public boolean isApproved() {
            return "approved".equalsIgnoreCase(status);
        }
    }
}
