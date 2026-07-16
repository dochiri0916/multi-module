package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorCodeMappingProviderTest {

    private static final TokenId TOKEN_ID = new TokenId("token-id-01");
    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("session-id-01");

    private final SecurityErrorCodeMappingProvider provider = new SecurityErrorCodeMappingProvider();

    @Test
    @DisplayName("모든 security와 refresh token 예외를 namespace API 오류로 해석한다")
    void resolvesSecurityAndRefreshTokenExceptionsToNamespacedErrors() {
        // given
        Map<RuntimeException, ApiErrorCode> expectations = new LinkedHashMap<>();
        expectations.put(
                new BadCredentialsException("internal"),
                ApiErrorCode.from(SecurityErrorCode.AUTHENTICATION_REQUIRED)
        );
        expectations.put(
                new AccessDeniedException("internal"),
                ApiErrorCode.from(SecurityErrorCode.ACCESS_DENIED)
        );
        expectations.put(
                InvalidTokenException.malformed(),
                ApiErrorCode.from(InvalidTokenException.malformed().code())
        );
        expectations.put(
                RefreshTokenNotFoundException.tokenNotFound(TOKEN_ID),
                ApiErrorCode.from(RefreshTokenNotFoundException.tokenNotFound(TOKEN_ID).code())
        );
        expectations.put(
                RefreshTokenSubjectMismatchException.subjectMismatch(TOKEN_ID),
                ApiErrorCode.from(RefreshTokenSubjectMismatchException.subjectMismatch(TOKEN_ID).code())
        );
        expectations.put(
                RefreshTokenExpirationMismatchException.expirationMismatch(TOKEN_ID),
                ApiErrorCode.from(RefreshTokenExpirationMismatchException.expirationMismatch(TOKEN_ID).code())
        );
        expectations.put(
                RefreshTokenInactiveException.inactive(TOKEN_ID, RefreshTokenStatus.REVOKED),
                ApiErrorCode.from(RefreshTokenInactiveException.inactive(TOKEN_ID, RefreshTokenStatus.REVOKED).code())
        );
        expectations.put(
                RefreshSessionNotFoundException.sessionNotFound(SESSION_ID),
                ApiErrorCode.from(RefreshSessionNotFoundException.sessionNotFound(SESSION_ID).code())
        );
        expectations.put(
                RefreshSessionInactiveException.inactive(SESSION_ID, RefreshSessionStatus.REVOKED),
                ApiErrorCode.from(
                        RefreshSessionInactiveException.inactive(SESSION_ID, RefreshSessionStatus.REVOKED).code()
                )
        );
        expectations.put(
                RefreshTokenRoleMismatchException.roleMismatch(SESSION_ID),
                ApiErrorCode.from(RefreshTokenRoleMismatchException.roleMismatch(SESSION_ID).code())
        );
        expectations.put(
                RefreshTokenReplayException.replayed(SESSION_ID, TOKEN_ID),
                ApiErrorCode.from(RefreshTokenReplayException.replayed(SESSION_ID, TOKEN_ID).code())
        );
        expectations.put(
                TokenCodecContractException.unexpectedTokenId(TOKEN_ID),
                ApiErrorCode.from(TokenCodecContractException.unexpectedTokenId(TOKEN_ID).code())
        );
        expectations.put(
                InvalidRefreshTokenRevocationCountException.negative(-1),
                ApiErrorCode.from(InvalidRefreshTokenRevocationCountException.negative(-1).code())
        );
        expectations.put(
                InvalidRefreshSessionCleanupBatchSizeException.invalid(0),
                ApiErrorCode.from(InvalidRefreshSessionCleanupBatchSizeException.invalid(0).code())
        );
        expectations.put(
                InvalidRefreshSessionCleanupCountException.invalid(-1),
                ApiErrorCode.from(InvalidRefreshSessionCleanupCountException.invalid(-1).code())
        );

        // when
        Map<RuntimeException, ApiErrorCode> resolved = expectations.keySet().stream()
                .collect(LinkedHashMap::new, (result, exception) -> result.put(
                        exception,
                        provider.resolve(exception).map(MappedApiError::code).orElseThrow()
                ), Map::putAll);

        // then
        assertThat(resolved).containsExactlyEntriesOf(expectations);
    }

    @Test
    @DisplayName("알 수 없는 예외는 security 오류로 임의 변환하지 않는다")
    void leavesUnknownExceptionUnresolved() {
        // given
        RuntimeException exception = new IllegalStateException("internal");

        // when
        boolean resolved = provider.resolve(exception).isPresent();

        // then
        assertThat(resolved).isFalse();
    }

    @Test
    @DisplayName("codec과 저장소 adapter 계약 오류는 내부 서버 오류로 매핑한다")
    void mapsAdapterContractFailuresToInternalServerError() {
        // given
        RuntimeException[] failures = {
                TokenCodecContractException.unexpectedTokenId(TOKEN_ID),
                InvalidRefreshTokenRevocationCountException.negative(-1),
                InvalidRefreshSessionCleanupBatchSizeException.invalid(0),
                InvalidRefreshSessionCleanupCountException.invalid(-1)
        };

        // when
        int[] statuses = java.util.Arrays.stream(failures)
                .map(provider::resolve)
                .map(Optional::orElseThrow)
                .mapToInt(mapped -> mapped.mapping().status().value())
                .toArray();

        // then
        assertThat(statuses).containsOnly(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
