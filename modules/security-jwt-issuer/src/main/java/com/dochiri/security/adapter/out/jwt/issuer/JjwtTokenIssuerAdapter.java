package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerProperties;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public final class JjwtTokenIssuerAdapter implements RotatingTokenIssuerPort {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CATEGORY = "category";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CATEGORY_ACCESS = "access";
    private static final String CATEGORY_REFRESH = "refresh";

    private final JwtIssuerProperties properties;

    @Override
    public IssuedTokenPair issue(
            AuthenticationSubject subject,
            AuthenticationRole role,
            TokenId refreshTokenId,
            CurrentTime issuedAt
    ) {
        Instant normalizedIssuedAt = Instant.ofEpochSecond(issuedAt.value().getEpochSecond());
        TokenExpiration accessExpiration = new TokenExpiration(
                normalizedIssuedAt.plus(properties.accessTokenTtl())
        );
        TokenExpiration refreshExpiration = new TokenExpiration(
                normalizedIssuedAt.plus(properties.refreshTokenTtl())
        );
        EncodedToken accessToken = encode(
                subject,
                role,
                CATEGORY_ACCESS,
                null,
                null,
                normalizedIssuedAt,
                accessExpiration.value()
        );
        EncodedToken refreshToken = encode(
                subject,
                role,
                CATEGORY_REFRESH,
                new RefreshSessionId(refreshTokenId.value()),
                refreshTokenId,
                normalizedIssuedAt,
                refreshExpiration.value()
        );
        return new IssuedTokenPair(accessToken, refreshToken, refreshTokenId, refreshExpiration);
    }

    @Override
    public IssuedTokenPair rotate(
            AuthenticationSubject subject,
            AuthenticationRole role,
            RefreshSessionId sessionId,
            TokenId refreshTokenId,
            TokenExpiration refreshTokenExpiresAt,
            CurrentTime issuedAt
    ) {
        Instant normalizedIssuedAt = Instant.ofEpochSecond(issuedAt.value().getEpochSecond());
        TokenExpiration accessExpiration = new TokenExpiration(
                normalizedIssuedAt.plus(properties.accessTokenTtl())
        );
        EncodedToken accessToken = encode(
                subject,
                role,
                CATEGORY_ACCESS,
                null,
                null,
                normalizedIssuedAt,
                accessExpiration.value()
        );
        EncodedToken refreshToken = encode(
                subject,
                role,
                CATEGORY_REFRESH,
                sessionId,
                refreshTokenId,
                normalizedIssuedAt,
                refreshTokenExpiresAt.value()
        );
        return new IssuedTokenPair(
                accessToken,
                refreshToken,
                refreshTokenId,
                refreshTokenExpiresAt
        );
    }

    private EncodedToken encode(
            AuthenticationSubject subject,
            AuthenticationRole role,
            String category,
            RefreshSessionId sessionId,
            TokenId tokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        var builder = Jwts.builder()
                .subject(subject.value())
                .claim(CLAIM_ROLE, role.value())
                .claim(CLAIM_CATEGORY, category)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
        if (tokenId != null) {
            builder.id(tokenId.value());
        }
        if (sessionId != null) {
            builder.claim(CLAIM_SESSION_ID, sessionId.value());
        }
        return new EncodedToken(builder.signWith(signingKey()).compact());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
