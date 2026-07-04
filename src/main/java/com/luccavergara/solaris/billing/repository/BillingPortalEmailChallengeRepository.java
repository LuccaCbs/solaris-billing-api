package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.BillingPortalEmailChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BillingPortalEmailChallengeRepository extends JpaRepository<BillingPortalEmailChallenge, Long> {

    Optional<BillingPortalEmailChallenge> findFirstByEmailNormalizedAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String emailNormalized,
            LocalDateTime now
    );
}
