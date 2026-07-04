package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.BillingPortalSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingPortalSessionRepository extends JpaRepository<BillingPortalSession, UUID> {

    Optional<BillingPortalSession> findByIdAndExpiresAtAfter(UUID id, java.time.LocalDateTime now);
}
