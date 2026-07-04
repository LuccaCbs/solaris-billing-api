package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.OrganizationMember;
import com.luccavergara.solaris.billing.entity.OrganizationMemberRole;
import com.luccavergara.solaris.billing.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.billing.entity.SolarisUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    @Query("""
            SELECT om FROM OrganizationMember om
            JOIN FETCH om.organization
            WHERE om.user = :user
              AND om.status = :status
              AND om.role IN :roles
            ORDER BY om.organization.razonSocial ASC
            """)
    List<OrganizationMember> findBillableMemberships(
            @Param("user") SolarisUser user,
            @Param("status") OrganizationMemberStatus status,
            @Param("roles") List<OrganizationMemberRole> roles
    );
}
