package com.dochiri.security.adapter.out.jwt;

import com.dochiri.security.adapter.out.jwt.configuration.JwtVerificationProperties;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.DecodedAccessToken;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.TokenExpiration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public final class JjwtAccessTokenVerifierAdapter implements AccessTokenVerifierPort {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CATEGORY = "category";
    private static final String CATEGORY_ACCESS = "access";

    private final JwtVerificationProperties properties;
    private final Clock clock;

    @Override
    public DecodedAccessToken verifyAccess(EncodedToken accessToken) {
        Claims claims = parse(accessToken);
        validateAccessCategory(claims);
        return new DecodedAccessToken(
                subjectOf(claims),
                roleOf(claims),
                expirationOf(claims)
        );
    }

    private Claims parse(EncodedToken token) {
        try {
            return Jwts.parser()
                    .clock(() -> Date.from(Instant.now(clock)))
                    .verifyWith(verificationKey())
                    .build()
                    .parseSignedClaims(token.value())
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw InvalidTokenException.expired();
        } catch (JwtException | IllegalArgumentException exception) {
            throw InvalidTokenException.malformed();
        }
    }

    private void validateAccessCategory(Claims claims) {
        String category = claims.get(CLAIM_CATEGORY, String.class);
        if (!CATEGORY_ACCESS.equals(category)) {
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

    private TokenExpiration expirationOf(Claims claims) {
        if (claims.getExpiration() == null) {
            throw InvalidTokenException.missingExpiration();
        }
        return new TokenExpiration(claims.getExpiration().toInstant());
    }

    private SecretKey verificationKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
