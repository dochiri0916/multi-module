package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class JwtProvider {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CATEGORY = "category";
    private static final String CATEGORY_ACCESS = "access";
    private static final String CATEGORY_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;
    private final Clock clock;

    public JwtProvider(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    public JwtProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = requireNonNull(clock);
    }

    public String generateAccessToken(Long userId, String role) {
        return generateToken(userId, role, CATEGORY_ACCESS, jwtProperties.accessExpiration());
    }

    public String generateRefreshToken(Long userId, String role) {
        return generateRefreshToken(userId, role, UUID.randomUUID().toString());
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now(clock).plusMillis(jwtProperties.refreshExpiration());
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new BadCredentialsException("만료된 JWT 토큰입니다.", exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("유효하지 않은 JWT 토큰입니다.", exception);
        }
    }

    public Long extractUserId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            log.warn("JWT 토큰에 sub 클레임이 없거나 비어 있습니다.");
            throw new BadCredentialsException("JWT 토큰에 유효한 sub 클레임이 포함되어야 합니다.");
        }

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            log.warn("JWT 토큰의 sub 클레임이 숫자가 아닙니다. subject: {}", subject);
            throw new BadCredentialsException("JWT 토큰에 유효한 sub 클레임이 포함되어야 합니다.", exception);
        }
    }

    public String extractRole(Claims claims) {
        return normalizeRole(claims.get(CLAIM_ROLE, String.class), claims.getSubject());
    }

    public boolean isAccessToken(Claims claims) {
        return CATEGORY_ACCESS.equals(claims.get(CLAIM_CATEGORY, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return CATEGORY_REFRESH.equals(claims.get(CLAIM_CATEGORY, String.class));
    }

    public String extractTokenId(Claims claims) {
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("JWT 토큰에 jti 클레임이 없거나 비어 있습니다. subject: {}", claims.getSubject());
            throw new BadCredentialsException("JWT 토큰에 유효한 jti 클레임이 포함되어야 합니다.");
        }
        return tokenId;
    }

    public Instant extractExpiration(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new BadCredentialsException("JWT 토큰에 만료 시각이 포함되어야 합니다.");
        }
        return expiration.toInstant();
    }

    String generateRefreshToken(Long userId, String role, String tokenId) {
        return generateToken(userId, role, CATEGORY_REFRESH, jwtProperties.refreshExpiration(), tokenId);
    }

    private String generateToken(Long userId, String role, String category, long expirationMillis) {
        return generateToken(userId, role, category, expirationMillis, null);
    }

    private String generateToken(Long userId, String role, String category, long expirationMillis, String tokenId) {
        requireNonNull(userId, "userId는 null일 수 없습니다.");
        String normalizedRole = normalizeRole(role, userId.toString());
        Instant now = Instant.now(clock);
        Instant expiration = now.plusMillis(expirationMillis);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, normalizedRole)
                .claim(CLAIM_CATEGORY, category)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration));

        if (tokenId != null && !tokenId.isBlank()) {
            builder.id(tokenId);
        }

        return builder
                .signWith(signingKey)
                .compact();
    }

    private String normalizeRole(String role, String subject) {
        if (role == null || role.isBlank()) {
            log.warn("JWT 토큰에 role 클레임이 없거나 비어 있습니다. subject: {}", subject);
            throw new BadCredentialsException("JWT 토큰에 유효한 role 클레임이 포함되어야 합니다.");
        }

        String trimmedRole = role.trim();

        if (trimmedRole.startsWith(ROLE_PREFIX)) {
            trimmedRole = trimmedRole.substring(ROLE_PREFIX.length());
        }

        if (trimmedRole.isBlank()) {
            log.warn("JWT 토큰의 role 클레임이 비어 있습니다. subject: {}", subject);
            throw new BadCredentialsException("JWT 토큰에 유효한 role 클레임이 포함되어야 합니다.");
        }

        return trimmedRole;
    }

}
