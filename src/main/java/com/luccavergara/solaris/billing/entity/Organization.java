package com.luccavergara.solaris.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "organizations")
@Getter
public class Organization {

    @Id
    private Long id;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "default_currency")
    private String defaultCurrency;
}
