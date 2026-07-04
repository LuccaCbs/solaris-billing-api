package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.BillingPortalPaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingPortalPaymentIntentRepository extends JpaRepository<BillingPortalPaymentIntent, Long> {

    Optional<BillingPortalPaymentIntent> findByExternalReference(String externalReference);
}
