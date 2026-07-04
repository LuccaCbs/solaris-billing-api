package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
