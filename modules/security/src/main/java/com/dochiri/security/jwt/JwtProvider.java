package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CATEGORY = "category";
    private static final String CATEGORY_ACCESS = "access";
    private static final String CATEGORY_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role) {
        return generateToken(userId, role, CATEGORY_ACCESS, jwtProperties.accessExpiration());
    }

    public String generateRefreshToken(Long userId, String role) {
        return generateToken(userId, role, CATEGORY_REFRESH, jwtProperties.refreshExpiration());
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now().plusMillis(jwtProperties.refreshExpiration());
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String extractRole(Claims claims) {
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null || role.isBlank()) {
            log.warn("JWT 토큰에 role 클레임이 없거나 비어 있습니다. subject: {}", claims.getSubject());
            throw new BadCredentialsException("JWT 토큰에 유효한 role 클레임이 포함되어야 합니다.");
        }
        return role;
    }

    public boolean isAccessToken(Claims claims) {
        return CATEGORY_ACCESS.equals(claims.get(CLAIM_CATEGORY, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return CATEGORY_REFRESH.equals(claims.get(CLAIM_CATEGORY, String.class));
    }

    private String generateToken(Long userId, String role, String category, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_CATEGORY, category)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }
}
