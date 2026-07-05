package com.luccavergara.solaris.billing.controller;

import com.luccavergara.solaris.billing.dto.*;
import com.luccavergara.solaris.billing.service.*;
import com.luccavergara.solaris.billing.repository.OrganizationRepository;
import com.luccavergara.solaris.billing.entity.Organization;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
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
    private final StoreAddonCheckoutService storeAddonCheckoutService;
    private final SubscriptionCheckoutService subscriptionCheckoutService;
    private final PlanCatalogService planCatalogService;
    private final BillingPromoCodeService billingPromoCodeService;
    private final OrganizationRepository organizationRepository;

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return billingPortalService.requestEmailVerification(request.getEmail());
    }

    @PostMapping("/confirm-email")
    public BillingSessionResponse confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        return billingPortalService.confirmEmail(request.getEmail(), request.getOtp());
    }

    @PostMapping("/session/prefill-from-app-token")
    public AppBillingPrefillResponse prefillFromAppToken(@Valid @RequestBody AppBillingTokenRequest request) {
        return billingPortalService.prefillFromAppToken(request.getBillingToken());
    }

    @PostMapping("/session/from-app-token")
    public BillingSessionResponse createSessionFromAppToken(@Valid @RequestBody AppBillingTokenRequest request) {
        return billingPortalService.createSessionFromAppToken(request.getBillingToken());
    }

    @GetMapping("/organizations")
    public List<BillingOrganizationResponse> listOrganizations(@RequestParam UUID sessionId) {
        return billingPortalService.listOrganizations(sessionId);
    }

    @GetMapping("/plans")
    public PlanCatalogResponse listPlans(
            @RequestParam UUID sessionId,
            @RequestParam Long organizationId
    ) {
        billingPortalService.assertValidSession(sessionId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return planCatalogService.getPublicPlans(organization);
    }

    @PostMapping("/promo-codes/preview")
    public PromoPreviewResponse previewPromoCode(@Valid @RequestBody PromoPreviewRequest request) {
        billingPortalService.assertValidSession(request.getSessionId());
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        return billingPromoCodeService.previewPromo(
                organization,
                request.getPlanCode(),
                request.getPromoCode()
        );
    }

    @PostMapping("/checkout/subscription")
    public SubscriptionCheckoutResponse checkoutSubscription(
            @Valid @RequestBody SubscriptionCheckoutRequest request
    ) {
        return subscriptionCheckoutService.createCheckout(
                request.getSessionId(),
                request.getOrganizationId(),
                request.getPlanCode(),
                request.getPromoCode()
        );
    }

    @GetMapping("/store-addon/quote")
    public StoreAddonQuoteResponse quoteStoreAddon(
            @RequestParam UUID sessionId,
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        return storeAddonCheckoutService.getQuote(sessionId, organizationId, quantity);
    }

    @PostMapping("/checkout/store-addon")
    public StoreAddonCheckoutResponse checkoutStoreAddon(@Valid @RequestBody StoreAddonCheckoutRequest request) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return storeAddonCheckoutService.createCheckout(
                request.getSessionId(),
                request.getOrganizationId(),
                quantity
        );
    }
}
