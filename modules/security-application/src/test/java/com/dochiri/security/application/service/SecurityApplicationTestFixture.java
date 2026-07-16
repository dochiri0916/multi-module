package com.dochiri.security.application.service;

import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionBulkRevocationPort;
import com.dochiri.security.application.port.out.RefreshSessionPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class SecurityApplicationTestFixture {

    static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    static final TokenId TOKEN_ID = new TokenId("token-id-01");
    static final RefreshSessionId SESSION_ID = new RefreshSessionId(TOKEN_ID.value());
    static final CurrentTime NOW = new CurrentTime(Instant.parse("2029-01-01T00:00:00Z"));
    static final TokenExpiration EXPIRATION = new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"));
    static final EncodedToken ACCESS_TOKEN = new EncodedToken("encoded-access-token");
    static final EncodedToken REFRESH_TOKEN = new EncodedToken("encoded-refresh-token");

    private SecurityApplicationTestFixture() {
    }

    static final class FixedTokenIssuer implements TokenIssuerPort {

        private final IssuedTokenPair issuedTokenPair = new IssuedTokenPair(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                TOKEN_ID,
                EXPIRATION
        );

        @Override
        public IssuedTokenPair issue(
                AuthenticationSubject subject,
                AuthenticationRole role,
                TokenId refreshTokenId,
                CurrentTime issuedAt
        ) {
            return issuedTokenPair;
        }
    }

    static final class FixedRefreshTokenVerifier implements RefreshTokenVerifierPort {

        private DecodedRefreshToken decodedRefreshTokenValue = new DecodedRefreshToken(
                SUBJECT,
                ROLE,
                TOKEN_ID,
                EXPIRATION
        );

        @Override
        public DecodedRefreshToken verifyRefresh(EncodedToken refreshToken) {
            return decodedRefreshTokenValue;
        }

        void decodedRefreshToken(DecodedRefreshToken value) {
            decodedRefreshTokenValue = value;
        }
    }

    static final class InMemoryRefreshSessionRepository implements RefreshSessionPort {

        private final Map<RefreshSessionId, RefreshSession> sessions = new LinkedHashMap<>();

        @Override
        public RefreshSession save(RefreshSession refreshSession) {
            sessions.put(refreshSession.sessionId(), refreshSession);
            return refreshSession;
        }

        @Override
        public Optional<RefreshSession> findBySessionIdForUpdate(RefreshSessionId sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public Optional<RefreshSession> findByCurrentTokenId(TokenId tokenId) {
            return sessions.values().stream()
                    .filter(session -> session.currentTokenId().equals(tokenId))
                    .findFirst();
        }

        void put(RefreshSession refreshSession) {
            sessions.put(refreshSession.sessionId(), refreshSession);
        }
    }

    static TokenIdGeneratorPort fixedTokenIdGenerator() {
        return () -> TOKEN_ID;
    }

    static CurrentTimePort fixedCurrentTime() {
        return () -> NOW;
    }

    static final class RecordingBulkRevocationPort implements RefreshSessionBulkRevocationPort {

        private int revokedCountResult;
        private AuthenticationSubject lastRevokedSubject;
        private RevokedAt lastRevokedAt;

        @Override
        public int revokeAll(AuthenticationSubject subject, RevokedAt revokedAt) {
            lastRevokedSubject = subject;
            lastRevokedAt = revokedAt;
            return revokedCountResult;
        }

        void result(int value) {
            revokedCountResult = value;
        }

        AuthenticationSubject revokedSubject() {
            return lastRevokedSubject;
        }

        RevokedAt revokedAt() {
            return lastRevokedAt;
        }
    }
}
