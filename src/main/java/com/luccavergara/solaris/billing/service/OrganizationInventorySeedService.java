package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.entity.OrganizationMember;
import com.luccavergara.solaris.billing.entity.OrganizationMemberRole;
import com.luccavergara.solaris.billing.entity.OrganizationMemberStatus;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.OrganizationMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationInventorySeedService {

    private static final String DEFAULT_CATEGORY_NAME = "General";

    @PersistenceContext
    private EntityManager entityManager;

    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationInventorySeedService(OrganizationMemberRepository organizationMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional
    public void seedDefaultCategoryIfMissing(Long organizationId) {
        Number existingCount = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM categories
                        WHERE organization_id = :organizationId
                          AND LOWER(name) = LOWER(:categoryName)
                        """)
                .setParameter("organizationId", organizationId)
                .setParameter("categoryName", DEFAULT_CATEGORY_NAME)
                .getSingleResult();

        if (existingCount.intValue() > 0) {
            return;
        }

        Long ownerUserId = resolveOwnerUserId(organizationId);

        entityManager.createNativeQuery("""
                        INSERT INTO categories (
                            name,
                            description,
                            created_at,
                            system_category,
                            user_id,
                            organization_id,
                            created_by_user_id
                        )
                        VALUES (
                            :categoryName,
                            'Default category',
                            NOW(),
                            TRUE,
                            :ownerUserId,
                            :organizationId,
                            :ownerUserId
                        )
                        """)
                .setParameter("categoryName", DEFAULT_CATEGORY_NAME)
                .setParameter("ownerUserId", ownerUserId)
                .setParameter("organizationId", organizationId)
                .executeUpdate();
    }

    private Long resolveOwnerUserId(Long organizationId) {
        return organizationMemberRepository
                .findFirstByOrganizationIdAndRoleAndStatus(
                        organizationId,
                        OrganizationMemberRole.OWNER,
                        OrganizationMemberStatus.ACTIVE
                )
                .map(member -> member.getUser().getId())
                .or(() -> organizationMemberRepository
                        .findFirstByOrganizationIdAndStatus(organizationId, OrganizationMemberStatus.ACTIVE)
                        .map(member -> member.getUser().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active organization member found to seed defaults"
                ));
    }
}
