package com.luccavergara.solaris.billing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class SolarisJwtService {

    public static final String CLAIM_ORG_ID = "orgId";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_PURPOSE = "purpose";
    public static final String PURPOSE_ORG_BILLING = "ORG_BILLING";

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public AppBillingClaims parseAppBillingToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Billing token is required");
        }

        Claims claims = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();

        if (!PURPOSE_ORG_BILLING.equals(claims.get(CLAIM_PURPOSE, String.class))) {
            throw new JwtException("Invalid billing token purpose");
        }

        Long organizationId = readLongClaim(claims, CLAIM_ORG_ID);
        Long userId = readLongClaim(claims, CLAIM_USER_ID);
        String email = claims.getSubject();

        if (organizationId == null || userId == null || email == null || email.isBlank()) {
            throw new JwtException("Billing token is missing required claims");
        }

        return new AppBillingClaims(organizationId, userId, email.trim());
    }

    private Long readLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            return Long.parseLong(string.trim());
        }

        return null;
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record AppBillingClaims(Long organizationId, Long userId, String email) {
    }
}
