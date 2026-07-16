package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerProperties;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.port.out.DecodedRefreshSessionToken;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public final class JjwtRefreshTokenVerifierAdapter implements RefreshSessionTokenVerifierPort {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CATEGORY = "category";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CATEGORY_REFRESH = "refresh";

    private final SecretKey verificationKey;
    private final Clock clock;

    public JjwtRefreshTokenVerifierAdapter(JwtIssuerProperties properties, Clock clock) {
        this.verificationKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public DecodedRefreshToken verifyRefresh(EncodedToken refreshToken) {
        Claims claims = parse(refreshToken);
        validateRefreshCategory(claims);
        return new DecodedRefreshToken(
                subjectOf(claims),
                roleOf(claims),
                tokenIdOf(claims),
                expirationOf(claims)
        );
    }

    @Override
    public DecodedRefreshSessionToken verifyRefreshSession(EncodedToken refreshToken) {
        Claims claims = parse(refreshToken);
        validateRefreshCategory(claims);
        return new DecodedRefreshSessionToken(
                refreshSessionIdOf(claims),
                subjectOf(claims),
                roleOf(claims),
                tokenIdOf(claims),
                expirationOf(claims)
        );
    }

    private Claims parse(EncodedToken token) {
        try {
            return Jwts.parser()
                    .clock(() -> Date.from(Instant.now(clock)))
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(token.value())
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw InvalidTokenException.expired();
        } catch (JwtException | IllegalArgumentException exception) {
            throw InvalidTokenException.malformed();
        }
    }

    private void validateRefreshCategory(Claims claims) {
        String category = claims.get(CLAIM_CATEGORY, String.class);
        if (!CATEGORY_REFRESH.equals(category)) {
            throw InvalidTokenException.invalidCategory();
        }
    }

    private AuthenticationSubject subjectOf(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw InvalidTokenException.missingSubject();
        }
        return new AuthenticationSubject(subject);
    }

    private AuthenticationRole roleOf(Claims claims) {
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null || role.isBlank()) {
            throw InvalidTokenException.missingRole();
        }
        return new AuthenticationRole(role);
    }

    private TokenId tokenIdOf(Claims claims) {
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw InvalidTokenException.missingTokenId();
        }
        return new TokenId(tokenId);
    }

    private RefreshSessionId refreshSessionIdOf(Claims claims) {
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        if (sessionId == null || sessionId.isBlank()) {
            throw InvalidTokenException.missingRefreshSessionId();
        }
        return new RefreshSessionId(sessionId);
    }

    private TokenExpiration expirationOf(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw InvalidTokenException.missingExpiration();
        }
        return new TokenExpiration(expiration.toInstant());
    }
}
