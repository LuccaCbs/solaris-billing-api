package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.PromoCodeRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRedemptionRepository extends JpaRepository<PromoCodeRedemption, Long> {

    boolean existsByPromoCodeIdAndOrganizationId(Long promoCodeId, Long organizationId);
}
