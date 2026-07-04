package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.dto.BillingOrganizationResponse;
import com.luccavergara.solaris.billing.dto.BillingSessionResponse;
import com.luccavergara.solaris.billing.dto.MessageResponse;
import com.luccavergara.solaris.billing.entity.*;
import com.luccavergara.solaris.billing.exception.ResourceNotFoundException;
import com.luccavergara.solaris.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingPortalService {

    private static final List<OrganizationMemberRole> BILLABLE_ROLES = List.of(
            OrganizationMemberRole.OWNER,
            OrganizationMemberRole.ADMIN
    );

    private final SolarisUserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final BillingPortalEmailChallengeRepository challengeRepository;
    private final BillingPortalSessionRepository sessionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.billing.otp-expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${application.billing.otp-max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${application.billing.session-expiration-minutes:30}")
    private int sessionExpirationMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MessageResponse requestEmailVerification(String email) {
        String normalizedEmail = normalizeEmail(email);

        SolarisUser user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No Solaris account found for this email"));

        List<OrganizationMember> memberships = organizationMemberRepository.findBillableMemberships(
                user,
                OrganizationMemberStatus.ACTIVE,
                BILLABLE_ROLES
        );

        if (memberships.isEmpty()) {
            throw new IllegalStateException("This email is not authorized to manage billing for any organization");
        }

        String otp = generateOtp();

        BillingPortalEmailChallenge challenge = BillingPortalEmailChallenge.builder()
                .emailNormalized(normalizedEmail)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();

        challengeRepository.save(challenge);
        emailService.sendBillingOtp(user.getEmail(), otp);

        return MessageResponse.builder()
                .message("Verification code sent")
                .build();
    }

    @Transactional
    public BillingSessionResponse confirmEmail(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);

        BillingPortalEmailChallenge challenge = challengeRepository
                .findFirstByEmailNormalizedAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        normalizedEmail,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));

        if (challenge.getAttempts() >= otpMaxAttempts) {
            throw new IllegalStateException("Too many failed attempts. Request a new code.");
        }

        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        challenge.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        SolarisUser user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No Solaris account found for this email"));

        UUID sessionId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(sessionExpirationMinutes);

        BillingPortalSession session = BillingPortalSession.builder()
                .id(sessionId)
                .user(user)
                .email(user.getEmail())
                .expiresAt(expiresAt)
                .build();

        sessionRepository.save(session);

        return BillingSessionResponse.builder()
                .sessionId(sessionId)
                .email(user.getEmail())
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BillingOrganizationResponse> listOrganizations(UUID sessionId) {
        BillingPortalSession session = sessionRepository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired billing session"));

        List<OrganizationMember> memberships = organizationMemberRepository.findBillableMemberships(
                session.getUser(),
                OrganizationMemberStatus.ACTIVE,
                BILLABLE_ROLES
        );

        return memberships.stream()
                .map(this::toOrganizationResponse)
                .toList();
    }

    private BillingOrganizationResponse toOrganizationResponse(OrganizationMember membership) {
        Organization organization = membership.getOrganization();
        String name = organization.getDisplayName() != null && !organization.getDisplayName().isBlank()
                ? organization.getDisplayName()
                : organization.getRazonSocial();

        return BillingOrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getRazonSocial())
                .displayName(name)
                .countryCode(organization.getCountryCode())
                .currency(organization.getDefaultCurrency())
                .role(membership.getRole())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
