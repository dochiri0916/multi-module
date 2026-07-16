package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMapping;
import com.dochiri.errorhandling.global.error.MappedApiError;
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
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RefreshSessionStatus;
import com.dochiri.security.domain.model.RefreshTokenStatus;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorCodeMappingProviderTest {

    private static final TokenId TOKEN_ID = new TokenId("token-id-01");
    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("session-id-01");

    private final SecurityErrorCodeMappingProvider provider = new SecurityErrorCodeMappingProvider();

    @Test
    @DisplayName("지원하는 보안 예외를 namespace API 오류 코드로 변환한다")
    void resolvesSupportedSecurityExceptionsToNamespacedApiCodes() {
        // given
        Map<RuntimeException, ApiErrorCode> expectedCodes = new LinkedHashMap<>();
        expectedCodes.put(
                new BadCredentialsException("internal"),
                ApiErrorCode.from(SecurityErrorCode.AUTHENTICATION_REQUIRED)
        );
        expectedCodes.put(
                new AccessDeniedException("internal"),
                ApiErrorCode.from(SecurityErrorCode.ACCESS_DENIED)
        );
        expectedCodes.put(
                InvalidTokenException.expired(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_EXPIRED)
        );
        expectedCodes.put(
                InvalidTokenException.malformed(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_MALFORMED)
        );
        expectedCodes.put(
                InvalidTokenException.invalidCategory(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID)
        );
        expectedCodes.put(
                InvalidTokenException.missingSubject(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING)
        );
        expectedCodes.put(
                InvalidTokenException.missingRole(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_ROLE_MISSING)
        );
        expectedCodes.put(
                InvalidTokenException.missingTokenId(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_ID_MISSING)
        );
        expectedCodes.put(
                InvalidTokenException.missingExpiration(),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING)
        );
        expectedCodes.put(
                InvalidTokenException.missingRefreshSessionId(),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_SESSION_ID_MISSING)
        );
        expectedCodes.put(
                TokenCodecContractException.unexpectedTokenId(TOKEN_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION)
        );
        expectedCodes.put(
                RefreshTokenNotFoundException.tokenNotFound(TOKEN_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND)
        );
        expectedCodes.put(
                RefreshTokenSubjectMismatchException.subjectMismatch(TOKEN_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH)
        );
        expectedCodes.put(
                RefreshTokenExpirationMismatchException.expirationMismatch(TOKEN_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_EXPIRATION_MISMATCH)
        );
        expectedCodes.put(
                RefreshTokenInactiveException.inactive(TOKEN_ID, RefreshTokenStatus.REVOKED),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_INACTIVE)
        );
        expectedCodes.put(
                InvalidRefreshTokenRevocationCountException.negative(-1),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_REVOCATION_COUNT_INVALID)
        );
        expectedCodes.put(
                RefreshSessionNotFoundException.sessionNotFound(SESSION_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_SESSION_NOT_FOUND)
        );
        expectedCodes.put(
                RefreshSessionInactiveException.inactive(SESSION_ID, RefreshSessionStatus.REVOKED),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_SESSION_INACTIVE)
        );
        expectedCodes.put(
                RefreshTokenRoleMismatchException.roleMismatch(SESSION_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_ROLE_MISMATCH)
        );
        expectedCodes.put(
                RefreshTokenReplayException.replayed(SESSION_ID, TOKEN_ID),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_TOKEN_REPLAYED)
        );
        expectedCodes.put(
                InvalidRefreshSessionCleanupBatchSizeException.invalid(0),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID)
        );
        expectedCodes.put(
                InvalidRefreshSessionCleanupCountException.invalid(-1),
                ApiErrorCode.from(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID)
        );

        // when
        Map<RuntimeException, ApiErrorCode> resolvedCodes = new LinkedHashMap<>();
        expectedCodes.forEach((exception, expectedCode) -> resolvedCodes.put(
                exception,
                provider.resolve(exception).map(MappedApiError::code).orElseThrow()
        ));

        // then
        assertThat(resolvedCodes).containsExactlyEntriesOf(expectedCodes);
    }

    @Test
    @DisplayName("알 수 없는 예외는 보안 오류 코드로 변환하지 않는다")
    void leavesUnknownExceptionUnresolved() {
        // given
        RuntimeException exception = new IllegalStateException("internal");

        // when
        boolean resolved = provider.resolve(exception).isPresent();

        // then
        assertThat(resolved).isFalse();
    }

    @Test
    @DisplayName("오류 코드 매핑은 내부 계약 오류를 500 상태로 변환한다")
    void mapsInternalContractFailuresToInternalServerError() {
        // given
        RuntimeException[] failures = {
                TokenCodecContractException.unexpectedTokenId(TOKEN_ID),
                InvalidRefreshTokenRevocationCountException.negative(-1),
                InvalidRefreshSessionCleanupBatchSizeException.invalid(0),
                InvalidRefreshSessionCleanupCountException.invalid(-1)
        };

        // when
        Map<ApiErrorCode, ApiErrorMapping> mappings = provider.errorCodeMappings();

        // then
        assertThat(mappings).hasSize(SecurityErrorCode.values().length + SecurityApplicationErrorCode.values().length);
        for (RuntimeException failure : failures) {
            MappedApiError mappedError = provider.resolve(failure).orElseThrow();
            assertThat(mappedError.mapping().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
