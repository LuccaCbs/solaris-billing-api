package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
public class SubscriptionPlan {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SubscriptionPlanCode code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "max_stores", nullable = false)
    private Integer maxStores;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
