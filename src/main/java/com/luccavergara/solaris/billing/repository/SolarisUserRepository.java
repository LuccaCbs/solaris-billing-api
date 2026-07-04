package com.luccavergara.solaris.billing.repository;

import com.luccavergara.solaris.billing.entity.SolarisUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolarisUserRepository extends JpaRepository<SolarisUser, Long> {

    Optional<SolarisUser> findByEmailIgnoreCase(String email);
}
