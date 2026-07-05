package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.billing.BillingPricingService;
import com.luccavergara.solaris.billing.dto.PromoPreviewResponse;
import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.PromoCodeRedemptionRepository;
import com.luccavergara.solaris.billing.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillingPromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeRedemptionRepository promoCodeRedemptionRepository;
    private final BillingPricingService billingPricingService;

    @Transactional(readOnly = true)
    public PromoPreviewResponse previewPromo(
            Organization organization,
            SubscriptionPlanCode selectedPlanCode,
            String rawPromoCode
    ) {
        PromoQuote quote = resolveQuote(organization, selectedPlanCode, rawPromoCode, null);

        return PromoPreviewResponse.builder()
                .valid(true)
                .message("Promo code applied")
                .planCode(selectedPlanCode)
                .effectivePlanCode(quote.effectivePlanCode())
                .originalPrice(quote.originalPrice())
                .finalPrice(quote.finalPrice())
                .discountPercent(quote.discountPercent())
                .currency(quote.currency())
                .requiresPayment(quote.finalPrice().compareTo(BigDecimal.ZERO) > 0)
                .promoCode(quote.promoCode().getCode())
                .build();
    }

    @Transactional
    public PromoQuote resolveQuote(
            Organization organization,
            SubscriptionPlanCode selectedPlanCode,
            String rawPromoCode,
            Long redeemedByUserId
    ) {
        if (!StringUtils.hasText(rawPromoCode)) {
            BigDecimal price = billingPricingService.getPlanPrice(organization, selectedPlanCode);
            String currency = billingPricingService.resolveCurrency(organization);

            return new PromoQuote(
                    null,
                    selectedPlanCode,
                    price,
                    price,
                    0,
                    currency,
                    null
            );
        }

        PromoCode promoCode = loadRedeemablePromo(rawPromoCode.trim(), organization.getId());
        BigDecimal originalPrice = billingPricingService.getPlanPrice(organization, selectedPlanCode);
        String currency = billingPricingService.resolveCurrency(organization);

        if (promoCode.getPromoType() != PromoCodeType.GRANT_PLAN) {
            throw new IllegalStateException("This promo code cannot be used for plan checkout");
        }

        SubscriptionPlanCode effectivePlan = promoCode.getGrantPlanCode() != null
                ? promoCode.getGrantPlanCode()
                : selectedPlanCode;

        return new PromoQuote(
                promoCode,
                effectivePlan,
                originalPrice,
                BigDecimal.ZERO,
                100,
                currency,
                redeemedByUserId
        );
    }

    @Transactional
    public void redeemPromoForCheckout(
            PromoCode promoCode,
            Organization organization,
            Long redeemedByUserId,
            SubscriptionPlanCode grantedPlanCode
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessValidUntil = promoCode.getDurationDays() != null
                ? now.plusDays(promoCode.getDurationDays())
                : null;

        PromoCodeRedemption redemption = PromoCodeRedemption.builder()
                .promoCodeId(promoCode.getId())
                .organization(organization)
                .redeemedByUserId(redeemedByUserId)
                .status(PromoRedemptionStatus.ACTIVE)
                .grantedPlanCode(grantedPlanCode)
                .accessValidFrom(now)
                .accessValidUntil(accessValidUntil)
                .createdAt(now)
                .build();

        promoCodeRedemptionRepository.save(redemption);
        promoCode.setRedemptionCount(promoCode.getRedemptionCount() + 1);
        promoCodeRepository.save(promoCode);
    }

    private PromoCode loadRedeemablePromo(String rawPromoCode, Long organizationId) {
        String normalized = normalizeCode(rawPromoCode);
        PromoCode promoCode = promoCodeRepository.findByCodeNormalized(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        LocalDateTime now = LocalDateTime.now();

        if (!promoCode.isRedeemableAt(now)) {
            throw new IllegalStateException("This promo code is not available for redemption");
        }

        if (promoCodeRedemptionRepository.existsByPromoCodeIdAndOrganizationId(promoCode.getId(), organizationId)) {
            throw new IllegalStateException("This organization has already redeemed this promo code");
        }

        return promoCode;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    public record PromoQuote(
            PromoCode promoCode,
            SubscriptionPlanCode effectivePlanCode,
            BigDecimal originalPrice,
            BigDecimal finalPrice,
            int discountPercent,
            String currency,
            Long redeemedByUserId
    ) {
    }
}
