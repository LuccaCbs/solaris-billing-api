package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "organization_members")
@Getter
public class OrganizationMember {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SolarisUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberStatus status;
}
