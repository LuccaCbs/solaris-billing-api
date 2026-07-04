package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class SolarisUser {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;
}
