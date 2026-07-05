package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "code_normalized", nullable = false, length = 64)
    private String codeNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 50)
    private PromoCodeType promoType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_plan_code", length = 50)
    private SubscriptionPlanCode grantPlanCode;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    private Integer redemptionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PromoCodeStatus status;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    public boolean isRedeemableAt(LocalDateTime now) {
        if (status != PromoCodeStatus.ACTIVE) {
            return false;
        }

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && !now.isBefore(validUntil)) {
            return false;
        }

        return maxRedemptions == null || redemptionCount < maxRedemptions;
    }
}
