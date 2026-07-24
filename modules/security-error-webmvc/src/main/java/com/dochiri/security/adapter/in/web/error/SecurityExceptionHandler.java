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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Objects;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class SecurityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(final AuthenticationException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        return problem(
                HttpStatus.UNAUTHORIZED,
                "/problems/authentication-required",
                "인증 필요",
                "인증이 필요합니다."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        return problem(
                HttpStatus.FORBIDDEN,
                "/problems/access-denied",
                "접근 거부",
                "접근 권한이 없습니다."
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(final InvalidTokenException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        return problem(
                HttpStatus.UNAUTHORIZED,
                "/problems/invalid-token",
                "토큰 검증 실패",
                "유효하지 않은 인증 토큰입니다."
        );
    }

    @ExceptionHandler({
            RefreshTokenNotFoundException.class,
            RefreshTokenSubjectMismatchException.class,
            RefreshTokenExpirationMismatchException.class,
            RefreshTokenInactiveException.class,
            RefreshSessionNotFoundException.class,
            RefreshSessionInactiveException.class,
            RefreshTokenRoleMismatchException.class,
            RefreshTokenReplayException.class
    })
    public ProblemDetail handleInvalidRefreshToken(final RuntimeException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        return problem(
                HttpStatus.UNAUTHORIZED,
                "/problems/invalid-refresh-token",
                "리프레시 토큰 검증 실패",
                "사용할 수 없는 리프레시 토큰입니다."
        );
    }

    @ExceptionHandler({
            TokenCodecContractException.class,
            InvalidRefreshTokenRevocationCountException.class,
            InvalidRefreshSessionCleanupBatchSizeException.class,
            InvalidRefreshSessionCleanupCountException.class
    })
    public ProblemDetail handleInternalSecurityError(final RuntimeException exception) {
        LOGGER.error("인증 처리 중 내부 계약 오류가 발생했습니다.", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/problems/security-processing-error",
                "인증 처리 오류",
                "인증 처리 중 오류가 발생했습니다."
        );
    }

    private static ProblemDetail problem(
            final HttpStatus status,
            final String type,
            final String title,
            final String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        return problem;
    }
}
