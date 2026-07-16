package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMapping;
import com.dochiri.errorhandling.global.error.ErrorCodeMappingProvider;
import com.dochiri.errorhandling.global.error.MappedApiError;
import com.dochiri.errorhandling.global.error.ProblemType;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public final class SecurityErrorCodeMappingProvider implements ErrorCodeMappingProvider {

    private static final Set<SecurityApplicationErrorCode> INTERNAL_ERROR_CODES = EnumSet.of(
            SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION,
            SecurityApplicationErrorCode.REFRESH_TOKEN_REVOCATION_COUNT_INVALID,
            SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID,
            SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID
    );
    private static final Map<ApiErrorCode, ApiErrorMapping> ERROR_CODE_MAPPINGS = mappings();

    @Override
    public Map<ApiErrorCode, ApiErrorMapping> errorCodeMappings() {
        return Map.copyOf(ERROR_CODE_MAPPINGS);
    }

    @Override
    public Optional<MappedApiError> resolve(RuntimeException exception) {
        if (exception instanceof AuthenticationException) {
            return resolve(SecurityErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (exception instanceof AccessDeniedException) {
            return resolve(SecurityErrorCode.ACCESS_DENIED);
        }
        if (exception instanceof InvalidTokenException invalidTokenException) {
            return resolve(invalidTokenException.code());
        }
        if (exception instanceof RefreshTokenNotFoundException notFoundException) {
            return resolve(notFoundException.code());
        }
        if (exception instanceof RefreshTokenSubjectMismatchException mismatchException) {
            return resolve(mismatchException.code());
        }
        if (exception instanceof RefreshTokenExpirationMismatchException mismatchException) {
            return resolve(mismatchException.code());
        }
        if (exception instanceof RefreshTokenInactiveException inactiveException) {
            return resolve(inactiveException.code());
        }
        if (exception instanceof RefreshSessionNotFoundException notFoundException) {
            return resolve(notFoundException.code());
        }
        if (exception instanceof RefreshSessionInactiveException inactiveException) {
            return resolve(inactiveException.code());
        }
        if (exception instanceof RefreshTokenRoleMismatchException mismatchException) {
            return resolve(mismatchException.code());
        }
        if (exception instanceof RefreshTokenReplayException replayException) {
            return resolve(replayException.code());
        }
        if (exception instanceof TokenCodecContractException contractException) {
            return resolve(contractException.code());
        }
        if (exception instanceof InvalidRefreshTokenRevocationCountException countException) {
            return resolve(countException.code());
        }
        if (exception instanceof InvalidRefreshSessionCleanupBatchSizeException batchSizeException) {
            return resolve(batchSizeException.code());
        }
        if (exception instanceof InvalidRefreshSessionCleanupCountException countException) {
            return resolve(countException.code());
        }
        return Optional.empty();
    }

    private static Map<ApiErrorCode, ApiErrorMapping> mappings() {
        Map<ApiErrorCode, ApiErrorMapping> mappings = new HashMap<>();
        mappings.put(
                ApiErrorCode.from(SecurityErrorCode.AUTHENTICATION_REQUIRED),
                new ApiErrorMapping(HttpStatus.UNAUTHORIZED, ProblemType.UNAUTHORIZED)
        );
        mappings.put(
                ApiErrorCode.from(SecurityErrorCode.ACCESS_DENIED),
                new ApiErrorMapping(HttpStatus.FORBIDDEN, ProblemType.FORBIDDEN)
        );
        EnumSet.allOf(SecurityApplicationErrorCode.class).forEach(errorCode -> mappings.put(
                ApiErrorCode.from(errorCode),
                applicationMapping(errorCode)
        ));
        return Map.copyOf(mappings);
    }

    private static ApiErrorMapping applicationMapping(SecurityApplicationErrorCode errorCode) {
        if (INTERNAL_ERROR_CODES.contains(errorCode)) {
            return new ApiErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, ProblemType.INTERNAL_ERROR);
        }
        return new ApiErrorMapping(HttpStatus.UNAUTHORIZED, ProblemType.UNAUTHORIZED);
    }
}
