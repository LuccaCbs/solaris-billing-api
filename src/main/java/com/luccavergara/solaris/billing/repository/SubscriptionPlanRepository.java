package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.SubscriptionPlan;
import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, SubscriptionPlanCode> {

    List<SubscriptionPlan> findByIsPublicTrueAndActiveTrueOrderBySortOrderAsc();
}
