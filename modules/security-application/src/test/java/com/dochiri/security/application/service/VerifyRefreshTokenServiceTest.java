package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshTokenExpirationMismatchException;
import com.dochiri.security.application.exception.RefreshTokenInactiveException;
import com.dochiri.security.application.exception.RefreshTokenNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenRoleMismatchException;
import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.in.VerifyRefreshTokenCommand;
import com.dochiri.security.application.port.in.VerifyRefreshTokenResult;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenExpiration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dochiri.security.application.service.SecurityApplicationTestFixture.EXPIRATION;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.NOW;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.REFRESH_TOKEN;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.ROLE;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SESSION_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SUBJECT;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.TOKEN_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.fixedCurrentTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifyRefreshTokenServiceTest {

    @Test
    @DisplayName("서명 정보와 저장된 Aggregate가 일치하는 활성 리프레시 토큰을 검증한다")
    void 서명_정보와_저장된_Aggregate가_일치하는_활성_리프레시_토큰을_검증한다() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(
                verifier,
                repository,
                fixedCurrentTime()
        );

        // when
        VerifyRefreshTokenResult result = service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN));

        // then
        assertThat(result.subject()).isEqualTo(SUBJECT);
        assertThat(result.tokenId()).isEqualTo(TOKEN_ID);
        assertThat(result.expiresAt()).isEqualTo(EXPIRATION);
    }

    @Test
    @DisplayName("저장되지 않은 리프레시 토큰은 식별자를 보존한 Application 예외로 거부한다")
    void 저장되지_않은_리프레시_토큰은_식별자를_보존한_Application_예외로_거부한다() {
        // given
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier(),
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository(),
                fixedCurrentTime()
        );

        // when & then
        assertThatThrownBy(() -> service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenNotFoundException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                });
    }

    @Test
    @DisplayName("JWT subject와 저장된 subject가 다르면 검증을 거부한다")
    void JWT_subject와_저장된_subject가_다르면_검증을_거부한다() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        verifier.decodedRefreshToken(new DecodedRefreshToken(
                new AuthenticationSubject("other-member"),
                ROLE,
                TOKEN_ID,
                EXPIRATION
        ));
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(verifier, repository, fixedCurrentTime());

        // when & then
        assertThatThrownBy(() -> service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenSubjectMismatchException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                });
    }

    @Test
    @DisplayName("폐기된 리프레시 토큰은 상태를 보존한 Application 예외로 거부한다")
    void 폐기된_리프레시_토큰은_상태를_보존한_Application_예외로_거부한다() {
        // given
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        RefreshSession revoked = RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION)
                .revoke(new RevokedAt(NOW.value()));
        repository.put(revoked);
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier(),
                repository,
                fixedCurrentTime()
        );

        // when & then
        assertThatThrownBy(() -> service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenInactiveException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_INACTIVE);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                });
    }

    @Test
    @DisplayName("JWT role과 세션 role 스냅샷이 다르면 session 식별자를 보존한 예외로 거부한다")
    void JWT_role과_세션_role_스냅샷이_다르면_session_식별자를_보존한_예외로_거부한다() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        verifier.decodedRefreshToken(new DecodedRefreshToken(
                SUBJECT,
                new AuthenticationRole("ADMIN"),
                TOKEN_ID,
                EXPIRATION
        ));
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(verifier, repository, fixedCurrentTime());

        // when & then
        assertThatThrownBy(() -> service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenRoleMismatchException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_ROLE_MISMATCH);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                });
    }

    @Test
    @DisplayName("JWT 만료 시각과 저장된 만료 시각이 다르면 식별자를 보존한 예외로 거부한다")
    void JWT_만료_시각과_저장된_만료_시각이_다르면_식별자를_보존한_예외로_거부한다() {
        // given
        SecurityApplicationTestFixture.FixedRefreshTokenVerifier verifier =
                new SecurityApplicationTestFixture.FixedRefreshTokenVerifier();
        verifier.decodedRefreshToken(new DecodedRefreshToken(
                SUBJECT,
                ROLE,
                TOKEN_ID,
                new TokenExpiration(EXPIRATION.value().plusSeconds(1))
        ));
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        repository.put(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
        VerifyRefreshTokenService service = new VerifyRefreshTokenService(verifier, repository, fixedCurrentTime());

        // when & then
        assertThatThrownBy(() -> service.execute(new VerifyRefreshTokenCommand(REFRESH_TOKEN)))
                .isInstanceOfSatisfying(RefreshTokenExpirationMismatchException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_EXPIRATION_MISMATCH);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                });
    }
}
