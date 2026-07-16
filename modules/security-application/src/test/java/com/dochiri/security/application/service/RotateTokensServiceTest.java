package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshSessionNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenReplayException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.in.RotateTokensCommand;
import com.dochiri.security.application.port.in.RotateTokensResult;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.DecodedRefreshSessionToken;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RevokedAt;
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
    void 활성_리프레시_세션을_rotation하면_같은_세션에_새_token_식별자를_저장한다() {
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
    void 저장된_리프레시_세션이_없으면_session_식별자를_보존한_예외를_던진다() {
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
    @DisplayName("이전 리프레시 token이 재사용되면 현재 세션을 폐기하고 재사용 예외를 던진다")
    void 이전_리프레시_token이_재사용되면_현재_세션을_폐기하고_재사용_예외를_던진다() {
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
        assertThat(repository.savedSession().isRevoked()).isTrue();
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

    private static final class InMemoryRefreshSessionRepository implements RefreshSessionRepositoryPort {

        private RefreshSession storedSession;
        private RefreshSession savedSession;

        @Override
        public RefreshSession save(RefreshSession refreshSession) {
            storedSession = refreshSession;
            savedSession = refreshSession;
            return refreshSession;
        }

        @Override
        public Optional<RefreshSession> findBySessionIdForUpdate(RefreshSessionId sessionId) {
            return Optional.ofNullable(storedSession);
        }

        @Override
        public Optional<RefreshSession> findByCurrentTokenId(TokenId tokenId) {
            if (storedSession == null || !storedSession.currentTokenId().equals(tokenId)) {
                return Optional.empty();
            }
            return Optional.of(storedSession);
        }

        void storedSession(RefreshSession value) {
            storedSession = value;
        }

        RefreshSession savedSession() {
            return savedSession;
        }
    }

    private static final class RecordingRotatingTokenCodec
            implements RotatingTokenIssuerPort, RefreshSessionTokenVerifierPort {

        private RefreshSessionId rotatedSessionId;
        private AuthenticationRole rotatedRole;

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
            rotatedSessionId = sessionId;
            rotatedRole = role;
            return rotatedPair();
        }

        @Override
        public DecodedRefreshSessionToken verifyRefreshSession(EncodedToken refreshToken) {
            return new DecodedRefreshSessionToken(
                    SESSION_ID,
                    SUBJECT,
                    ROLE,
                    CURRENT_TOKEN_ID,
                    EXPIRATION
            );
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
            return rotatedSessionId;
        }

        AuthenticationRole rotatedRole() {
            return rotatedRole;
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
