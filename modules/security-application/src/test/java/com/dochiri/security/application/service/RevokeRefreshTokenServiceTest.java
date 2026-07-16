package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.in.RevokeRefreshTokenCommand;
import com.dochiri.security.application.port.in.RevokeRefreshTokenResult;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RevokedAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.dochiri.security.application.service.SecurityApplicationTestFixture.EXPIRATION;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.REFRESH_TOKEN;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.ROLE;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SESSION_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SUBJECT;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.TOKEN_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.fixedCurrentTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevokeRefreshTokenServiceTest {

    @Test
    @DisplayName("저장된 리프레시 토큰을 불변 상태 전이로 폐기하고 저장한다")
    void revokesStoredRefreshTokenWithImmutableTransition() {
        // given
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        RevokeRefreshTokenService service = new RevokeRefreshTokenService(
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier(),
                repository,
                fixedCurrentTime()
        );

        // when
        RevokeRefreshTokenResult result = service.execute(new RevokeRefreshTokenCommand(REFRESH_TOKEN));

        // then
        assertThat(result.revoked()).isTrue();
        assertThat(repository.findByCurrentTokenId(TOKEN_ID))
                .get()
                .extracting(session -> session.status().isRevoked())
                .isEqualTo(true);
    }

    @Test
    @DisplayName("저장되지 않은 리프레시 토큰 폐기는 false를 반환한다")
    void returnsFalseWhenRefreshTokenIsNotStored() {
        // given
        RevokeRefreshTokenService service = new RevokeRefreshTokenService(
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier(),
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository(),
                fixedCurrentTime()
        );

        // when
        RevokeRefreshTokenResult result = service.execute(new RevokeRefreshTokenCommand(REFRESH_TOKEN));

        // then
        assertThat(result.revoked()).isFalse();
    }

    @Test
    @DisplayName("JWT subject와 저장된 subject가 다르면 토큰 식별자를 보존한 예외로 거부한다")
    void rejectsDifferentJwtAndStoredSubjectsWithTokenId() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        verifier.decodedRefreshToken(new DecodedRefreshToken(
                new AuthenticationSubject("another-member"),
                ROLE,
                TOKEN_ID,
                EXPIRATION
        ));
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        RevokeRefreshTokenService service = new RevokeRefreshTokenService(verifier, repository, fixedCurrentTime());

        // when & then
        assertThatThrownBy(() -> service.execute(new RevokeRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenSubjectMismatchException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                });
    }

    @Test
    @DisplayName("이미 폐기된 리프레시 토큰은 다시 저장하지 않고 false를 반환한다")
    void returnsFalseWithoutSavingAlreadyRevokedRefreshToken() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        RevokedAt revokedAt = new RevokedAt(Instant.parse("2028-01-01T00:00:00Z"));
        RefreshSession revoked = RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION)
                .revoke(revokedAt);
        repository.put(revoked);
        RevokeRefreshTokenService service = new RevokeRefreshTokenService(verifier, repository, fixedCurrentTime());

        // when
        RevokeRefreshTokenResult result = service.execute(new RevokeRefreshTokenCommand(REFRESH_TOKEN));

        // then
        assertThat(result.revoked()).isFalse();
        assertThat(repository.findByCurrentTokenId(TOKEN_ID)).containsSame(revoked);
    }
}
