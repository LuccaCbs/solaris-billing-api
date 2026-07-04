package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.Organization;
import com.luccavergara.solaris.billing.entity.OrganizationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    Optional<OrganizationSubscription> findByOrganization(Organization organization);

    Optional<OrganizationSubscription> findByOrganizationId(Long organizationId);
}
