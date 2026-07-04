package com.luccavergara.solaris.billing.entity;

public enum OrganizationMemberRole {
    OWNER(4),
    ADMIN(3),
    MANAGER(2),
    CASHIER(1);

    private final int privilegeLevel;

    OrganizationMemberRole(int privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    public int getPrivilegeLevel() {
        return privilegeLevel;
    }
}
