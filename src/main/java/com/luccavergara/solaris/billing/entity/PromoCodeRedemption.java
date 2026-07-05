package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_code_redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promo_code_id", nullable = false)
    private Long promoCodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "redeemed_by_user_id", nullable = false)
    private Long redeemedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PromoRedemptionStatus status = PromoRedemptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_plan_code")
    private SubscriptionPlanCode grantedPlanCode;

    @Column(name = "access_valid_from", nullable = false)
    private LocalDateTime accessValidFrom;

    @Column(name = "access_valid_until")
    private LocalDateTime accessValidUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
