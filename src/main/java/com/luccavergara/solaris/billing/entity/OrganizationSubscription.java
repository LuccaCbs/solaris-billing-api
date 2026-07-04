package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false)
    @Builder.Default
    private SubscriptionPlanCode planCode = SubscriptionPlanCode.POS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIALING;

    @Column(name = "max_stores", nullable = false)
    @Builder.Default
    private Integer maxStores = 1;

    @Column(name = "extra_stores_purchased", nullable = false)
    @Builder.Default
    private Integer extraStoresPurchased = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_provider", nullable = false)
    @Builder.Default
    private BillingProvider billingProvider = BillingProvider.NONE;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isBillingActive() {
        return status == SubscriptionStatus.TRIALING || status == SubscriptionStatus.ACTIVE;
    }
}
