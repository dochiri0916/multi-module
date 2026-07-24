package com.dochiri.security.adapter.in.web.error;

import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupBatchSizeException;
import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupCountException;
import com.dochiri.security.application.exception.InvalidRefreshTokenRevocationCountException;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.exception.RefreshSessionInactiveException;
import com.dochiri.security.application.exception.RefreshSessionNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenExpirationMismatchException;
import com.dochiri.security.application.exception.RefreshTokenInactiveException;
import com.dochiri.security.application.exception.RefreshTokenNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenReplayException;
import com.dochiri.security.application.exception.RefreshTokenRoleMismatchException;
import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RefreshSessionStatus;
import com.dochiri.security.domain.model.RefreshTokenStatus;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlerTest {

    private static final TokenId TOKEN_ID = new TokenId("token-id-01");
    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("session-id-01");

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Test
    @DisplayName("인증 예외는 인증 필요 ProblemDetail로 반환한다")
    void handlesAuthenticationException() {
        // given
        BadCredentialsException exception = new BadCredentialsException("internal-secret-message");

        // when
        ProblemDetail problem = handler.handleAuthentication(exception);

        // then
        assertProblem(
                problem,
                HttpStatus.UNAUTHORIZED,
                "/problems/authentication-required",
                "인증 필요",
                "인증이 필요합니다."
        );
        assertThat(problem.getDetail()).doesNotContain(exception.getMessage());
    }

    @Test
    @DisplayName("인가 예외는 접근 거부 ProblemDetail로 반환한다")
    void handlesAccessDeniedException() {
        // given
        AccessDeniedException exception = new AccessDeniedException("internal-secret-message");

        // when
        ProblemDetail problem = handler.handleAccessDenied(exception);

        // then
        assertProblem(
                problem,
                HttpStatus.FORBIDDEN,
                "/problems/access-denied",
                "접근 거부",
                "접근 권한이 없습니다."
        );
        assertThat(problem.getDetail()).doesNotContain(exception.getMessage());
    }

    @Test
    @DisplayName("토큰 검증 예외는 동일한 인증 실패 ProblemDetail로 반환한다")
    void handlesInvalidTokenExceptions() {
        // given
        InvalidTokenException[] exceptions = {
                InvalidTokenException.expired(),
                InvalidTokenException.malformed(),
                InvalidTokenException.invalidCategory(),
                InvalidTokenException.missingSubject(),
                InvalidTokenException.missingRole(),
                InvalidTokenException.missingTokenId(),
                InvalidTokenException.missingExpiration(),
                InvalidTokenException.missingRefreshSessionId()
        };

        // when
        ProblemDetail[] problems = java.util.Arrays.stream(exceptions)
                .map(handler::handleInvalidToken)
                .toArray(ProblemDetail[]::new);

        // then
        assertThat(problems).allSatisfy(problem -> assertProblem(
                problem,
                HttpStatus.UNAUTHORIZED,
                "/problems/invalid-token",
                "토큰 검증 실패",
                "유효하지 않은 인증 토큰입니다."
        ));
    }

    @Test
    @DisplayName("리프레시 토큰 정책 예외는 동일한 인증 실패 ProblemDetail로 반환한다")
    void handlesInvalidRefreshTokenExceptions() {
        // given
        RuntimeException[] exceptions = {
                RefreshTokenNotFoundException.tokenNotFound(TOKEN_ID),
                RefreshTokenSubjectMismatchException.subjectMismatch(TOKEN_ID),
                RefreshTokenExpirationMismatchException.expirationMismatch(TOKEN_ID),
                RefreshTokenInactiveException.inactive(TOKEN_ID, RefreshTokenStatus.REVOKED),
                RefreshSessionNotFoundException.sessionNotFound(SESSION_ID),
                RefreshSessionInactiveException.inactive(SESSION_ID, RefreshSessionStatus.REVOKED),
                RefreshTokenRoleMismatchException.roleMismatch(SESSION_ID),
                RefreshTokenReplayException.replayed(SESSION_ID, TOKEN_ID)
        };

        // when
        ProblemDetail[] problems = java.util.Arrays.stream(exceptions)
                .map(handler::handleInvalidRefreshToken)
                .toArray(ProblemDetail[]::new);

        // then
        assertThat(problems).allSatisfy(problem -> assertProblem(
                problem,
                HttpStatus.UNAUTHORIZED,
                "/problems/invalid-refresh-token",
                "리프레시 토큰 검증 실패",
                "사용할 수 없는 리프레시 토큰입니다."
        ));
    }

    @Test
    @DisplayName("내부 보안 계약 예외는 정보를 숨긴 서버 오류 ProblemDetail로 반환한다")
    void handlesInternalSecurityExceptions() {
        // given
        RuntimeException[] exceptions = {
                TokenCodecContractException.unexpectedTokenId(TOKEN_ID),
                InvalidRefreshTokenRevocationCountException.negative(-1),
                InvalidRefreshSessionCleanupBatchSizeException.invalid(0),
                InvalidRefreshSessionCleanupCountException.invalid(-1)
        };

        // when
        ProblemDetail[] problems = java.util.Arrays.stream(exceptions)
                .map(handler::handleInternalSecurityError)
                .toArray(ProblemDetail[]::new);

        // then
        assertThat(problems).allSatisfy(problem -> assertProblem(
                problem,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/problems/security-processing-error",
                "인증 처리 오류",
                "인증 처리 중 오류가 발생했습니다."
        ));
    }

    private void assertProblem(
            ProblemDetail problem,
            HttpStatus status,
            String type,
            String title,
            String detail
    ) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getType()).isEqualTo(URI.create(type));
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getProperties()).isNullOrEmpty();
    }
}
