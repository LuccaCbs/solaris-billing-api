package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCodeNormalized(String codeNormalized);
}
