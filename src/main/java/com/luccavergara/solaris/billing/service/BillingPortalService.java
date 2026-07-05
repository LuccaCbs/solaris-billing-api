package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.dto.AppBillingPrefillResponse;
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
import com.luccavergara.solaris.billing.security.SolarisJwtService;
import com.luccavergara.solaris.billing.util.EmailNormalizer;
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
    private final SolarisJwtService solarisJwtService;

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
    public AppBillingPrefillResponse prefillFromAppToken(String billingToken) {
        SolarisJwtService.AppBillingClaims claims = solarisJwtService.parseAppBillingToken(billingToken);
        assertBillableAppTokenClaims(claims);

        return AppBillingPrefillResponse.builder()
                .email(claims.email())
                .preferredOrganizationId(claims.organizationId())
                .build();
    }

    @Transactional
    public BillingSessionResponse createSessionFromAppToken(String billingToken) {
        SolarisJwtService.AppBillingClaims claims = solarisJwtService.parseAppBillingToken(billingToken);
        assertBillableAppTokenClaims(claims);

        SolarisUser user = userRepository.findById(claims.userId())
                .filter(found -> found.getEmail().equalsIgnoreCase(claims.email()))
                .orElseThrow(() -> new IllegalArgumentException("Billing token is no longer valid for this user"));

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
                .preferredOrganizationId(claims.organizationId())
                .build();
    }

    @Transactional(readOnly = true)
    public void assertValidSession(UUID sessionId) {
        sessionRepository.findByIdAndExpiresAtAfter(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired billing session"));
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

    private void assertBillableAppTokenClaims(SolarisJwtService.AppBillingClaims claims) {
        OrganizationMemberRole role = organizationMemberRepository
                .findRoleByOrganizationIdAndUserEmailIgnoreCaseAndStatus(
                        claims.organizationId(),
                        claims.email(),
                        OrganizationMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException("Billing token is no longer valid for this organization"));

        if (role.getPrivilegeLevel() < OrganizationMemberRole.ADMIN.getPrivilegeLevel()) {
            throw new IllegalStateException("This email is not authorized to manage billing for this organization");
        }

        userRepository.findById(claims.userId())
                .filter(found -> found.getEmail().equalsIgnoreCase(claims.email()))
                .orElseThrow(() -> new IllegalArgumentException("Billing token is no longer valid for this user"));
    }

    private String normalizeEmail(String email) {
        String normalized = EmailNormalizer.normalize(email);
        if (!EmailNormalizer.isValid(normalized)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return normalized;
    }

    private String generateOtp() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
