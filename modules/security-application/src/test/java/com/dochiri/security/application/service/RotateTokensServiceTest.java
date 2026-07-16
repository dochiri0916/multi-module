package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshSessionInactiveException;
import com.dochiri.security.application.exception.RefreshSessionNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenExpirationMismatchException;
import com.dochiri.security.application.exception.RefreshTokenRoleMismatchException;
import com.dochiri.security.application.exception.RefreshTokenReplayException;
import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.application.port.in.RotateTokensCommand;
import com.dochiri.security.application.port.in.RotateTokensResult;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.DecodedRefreshSessionToken;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.RefreshSessionStatus;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RotateTokensServiceTest {

    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("session-id-01");
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    private static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    private static final TokenId CURRENT_TOKEN_ID = new TokenId("token-id-01");
    private static final TokenId ROTATED_TOKEN_ID = new TokenId("token-id-02");
    private static final TokenExpiration EXPIRATION =
            new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"));
    private static final CurrentTime NOW = new CurrentTime(Instant.parse("2029-01-01T00:00:00Z"));
    private static final EncodedToken PRESENTED_REFRESH_TOKEN = new EncodedToken("presented-refresh-token");
    private static final EncodedToken ROTATED_ACCESS_TOKEN = new EncodedToken("rotated-access-token");
    private static final EncodedToken ROTATED_REFRESH_TOKEN = new EncodedToken("rotated-refresh-token");

    @Test
    @DisplayName("활성 리프레시 세션을 rotation하면 같은 세션에 새 token 식별자를 저장한다")
    void rotatesActiveRefreshSessionAndStoresReplacementTokenId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        InMemoryRefreshSessionRepository repository = repositoryWith(issueSession());
        RotateTokensService service = service(codec, repository);

        // when
        RotateTokensResult result = service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN));

        // then
        assertThat(result.accessToken()).isEqualTo(ROTATED_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(ROTATED_REFRESH_TOKEN);
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(EXPIRATION);
        assertThat(repository.savedSession().sessionId()).isEqualTo(SESSION_ID);
        assertThat(repository.savedSession().currentTokenId()).isEqualTo(ROTATED_TOKEN_ID);
        assertThat(codec.rotatedSessionId()).isEqualTo(SESSION_ID);
        assertThat(codec.rotatedRole()).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("저장된 리프레시 세션이 없으면 session 식별자를 보존한 예외를 던진다")
    void preservesSessionIdWhenRefreshSessionIsMissing() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        InMemoryRefreshSessionRepository repository = new InMemoryRefreshSessionRepository();
        RotateTokensService service = service(codec, repository);

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshSessionNotFoundException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_NOT_FOUND);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                });
    }

    @Test
    @DisplayName("폐기된 리프레시 세션은 상태와 세션 식별자를 보존한 예외로 거부한다")
    void rejectsInactiveRefreshSessionWithStatusAndSessionId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        RefreshSession revokedSession = issueSession().revoke(new RevokedAt(NOW.value()));
        RotateTokensService service = service(codec, repositoryWith(revokedSession));

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshSessionInactiveException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_INACTIVE);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                    assertThat(exception.status()).isEqualTo(RefreshSessionStatus.REVOKED);
                });
    }

    @Test
    @DisplayName("JWT subject가 세션 subject와 다르면 토큰 식별자를 보존한 예외로 거부한다")
    void rejectsSubjectMismatchWithTokenId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        codec.decodedSessionToken(new DecodedRefreshSessionToken(
                SESSION_ID,
                new AuthenticationSubject("other-member"),
                ROLE,
                CURRENT_TOKEN_ID,
                EXPIRATION
        ));
        RotateTokensService service = service(codec, repositoryWith(issueSession()));

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenSubjectMismatchException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH);
                    assertThat(exception.tokenId()).isEqualTo(CURRENT_TOKEN_ID);
                });
    }

    @Test
    @DisplayName("JWT role이 세션 role과 다르면 세션 식별자를 보존한 예외로 거부한다")
    void rejectsRoleMismatchWithSessionId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        codec.decodedSessionToken(new DecodedRefreshSessionToken(
                SESSION_ID,
                SUBJECT,
                new AuthenticationRole("ADMIN"),
                CURRENT_TOKEN_ID,
                EXPIRATION
        ));
        RotateTokensService service = service(codec, repositoryWith(issueSession()));

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenRoleMismatchException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_ROLE_MISMATCH);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                });
    }

    @Test
    @DisplayName("JWT 만료 시각이 세션 만료 시각과 다르면 토큰 식별자를 보존한 예외로 거부한다")
    void rejectsExpirationMismatchWithTokenId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        codec.decodedSessionToken(new DecodedRefreshSessionToken(
                SESSION_ID,
                SUBJECT,
                ROLE,
                CURRENT_TOKEN_ID,
                new TokenExpiration(EXPIRATION.value().plusSeconds(1))
        ));
        RotateTokensService service = service(codec, repositoryWith(issueSession()));

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenExpirationMismatchException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_EXPIRATION_MISMATCH);
                    assertThat(exception.tokenId()).isEqualTo(CURRENT_TOKEN_ID);
                });
    }

    @Test
    @DisplayName("Token issuer가 다른 리프레시 토큰 식별자를 반환하면 계약 위반으로 거부한다")
    void rejectsUnexpectedRotatedRefreshTokenId() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        codec.issuedTokenPair(new IssuedTokenPair(
                ROTATED_ACCESS_TOKEN,
                ROTATED_REFRESH_TOKEN,
                new TokenId("unexpected-token-id"),
                EXPIRATION
        ));
        InMemoryRefreshSessionRepository repository = repositoryWith(issueSession());
        RotateTokensService service = service(codec, repository);

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(TokenCodecContractException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION);
                    assertThat(exception.tokenId()).isEqualTo(ROTATED_TOKEN_ID);
                });
        assertThat(repository.savedSession()).isNull();
    }

    @Test
    @DisplayName("이전 리프레시 token이 재사용되면 현재 세션을 폐기하고 재사용 예외를 던진다")
    void revokesSessionWhenPreviousRefreshTokenIsReplayed() {
        // given
        RecordingRotatingTokenCodec codec = new RecordingRotatingTokenCodec();
        RefreshSession rotatedSession = issueSession().rotate(CURRENT_TOKEN_ID, ROTATED_TOKEN_ID, NOW);
        InMemoryRefreshSessionRepository repository = repositoryWith(rotatedSession);
        RotateTokensService service = service(codec, repository);

        // when & then
        assertThatThrownBy(() -> service.execute(new RotateTokensCommand(PRESENTED_REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenReplayException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_REPLAYED);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                    assertThat(exception.replayedTokenId()).isEqualTo(CURRENT_TOKEN_ID);
                });
        assertThat(repository.savedSession().status().isRevoked()).isTrue();
        assertThat(repository.savedSession().revokedAt()).isEqualTo(new RevokedAt(NOW.value()));
    }

    private RotateTokensService service(
            RecordingRotatingTokenCodec codec,
            InMemoryRefreshSessionRepository repository
    ) {
        return new RotateTokensService(codec, codec, repository, () -> ROTATED_TOKEN_ID, () -> NOW);
    }

    private InMemoryRefreshSessionRepository repositoryWith(RefreshSession session) {
        InMemoryRefreshSessionRepository repository = new InMemoryRefreshSessionRepository();
        repository.storedSession(session);
        return repository;
    }

    private RefreshSession issueSession() {
        return RefreshSession.issue(SESSION_ID, CURRENT_TOKEN_ID, SUBJECT, ROLE, EXPIRATION);
    }

    private static final class InMemoryRefreshSessionRepository implements RefreshSessionPort {

        private RefreshSession storedSessionState;
        private RefreshSession savedSessionState;

        @Override
        public RefreshSession save(RefreshSession refreshSession) {
            storedSessionState = refreshSession;
            savedSessionState = refreshSession;
            return refreshSession;
        }

        @Override
        public Optional<RefreshSession> findBySessionIdForUpdate(RefreshSessionId sessionId) {
            return Optional.ofNullable(storedSessionState);
        }

        @Override
        public Optional<RefreshSession> findByCurrentTokenId(TokenId tokenId) {
            if (storedSessionState == null || !storedSessionState.currentTokenId().equals(tokenId)) {
                return Optional.empty();
            }
            return Optional.of(storedSessionState);
        }

        void storedSession(RefreshSession value) {
            storedSessionState = value;
        }

        RefreshSession savedSession() {
            return savedSessionState;
        }
    }

    private static final class RecordingRotatingTokenCodec
            implements RotatingTokenIssuerPort, RefreshSessionTokenVerifierPort {

        private RefreshSessionId lastRotatedSessionId;
        private AuthenticationRole lastRotatedRole;
        private DecodedRefreshSessionToken decodedSessionTokenValue = new DecodedRefreshSessionToken(
                SESSION_ID,
                SUBJECT,
                ROLE,
                CURRENT_TOKEN_ID,
                EXPIRATION
        );
        private IssuedTokenPair configuredTokenPair;

        @Override
        public IssuedTokenPair issue(
                AuthenticationSubject subject,
                AuthenticationRole role,
                TokenId refreshTokenId,
                CurrentTime issuedAt
        ) {
            return rotatedPair();
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
            lastRotatedSessionId = sessionId;
            lastRotatedRole = role;
            return configuredTokenPair == null ? rotatedPair() : configuredTokenPair;
        }

        @Override
        public DecodedRefreshSessionToken verifyRefreshSession(EncodedToken refreshToken) {
            return decodedSessionTokenValue;
        }

        @Override
        public DecodedRefreshToken verifyRefresh(EncodedToken refreshToken) {
            return new DecodedRefreshToken(
                    SUBJECT,
                    ROLE,
                    CURRENT_TOKEN_ID,
                    EXPIRATION
            );
        }

        RefreshSessionId rotatedSessionId() {
            return lastRotatedSessionId;
        }

        AuthenticationRole rotatedRole() {
            return lastRotatedRole;
        }

        void decodedSessionToken(DecodedRefreshSessionToken value) {
            decodedSessionTokenValue = value;
        }

        void issuedTokenPair(IssuedTokenPair value) {
            configuredTokenPair = value;
        }

        private IssuedTokenPair rotatedPair() {
            return new IssuedTokenPair(
                    ROTATED_ACCESS_TOKEN,
                    ROTATED_REFRESH_TOKEN,
                    ROTATED_TOKEN_ID,
                    EXPIRATION
            );
        }
    }
}
