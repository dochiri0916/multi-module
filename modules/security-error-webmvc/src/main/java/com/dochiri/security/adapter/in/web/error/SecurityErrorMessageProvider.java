package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMessage;
import com.dochiri.errorhandling.global.error.ApiErrorMessageProvider;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Component
public final class SecurityErrorMessageProvider implements ApiErrorMessageProvider {

    private static final Map<ApiErrorCode, ApiErrorMessage> ERROR_MESSAGES = messages();

    @Override
    public Map<ApiErrorCode, ApiErrorMessage> errorMessages() {
        return Map.copyOf(ERROR_MESSAGES);
    }

    private static Map<ApiErrorCode, ApiErrorMessage> messages() {
        Map<ApiErrorCode, ApiErrorMessage> messages = new HashMap<>();
        messages.put(
                ApiErrorCode.from(SecurityErrorCode.AUTHENTICATION_REQUIRED),
                new ApiErrorMessage("인증 필요", "인증이 필요합니다.")
        );
        messages.put(
                ApiErrorCode.from(SecurityErrorCode.ACCESS_DENIED),
                new ApiErrorMessage("접근 거부", "접근 권한이 없습니다.")
        );
        applicationMessages().forEach((errorCode, message) ->
                messages.put(ApiErrorCode.from(errorCode), message));
        return Map.copyOf(messages);
    }

    private static Map<SecurityApplicationErrorCode, ApiErrorMessage> applicationMessages() {
        Map<SecurityApplicationErrorCode, ApiErrorMessage> messages =
                new EnumMap<>(SecurityApplicationErrorCode.class);
        ApiErrorMessage invalidToken = new ApiErrorMessage("토큰 검증 실패", "유효하지 않은 인증 토큰입니다.");
        messages.put(SecurityApplicationErrorCode.TOKEN_EXPIRED, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_MALFORMED, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_ROLE_MISSING, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_ID_MISSING, invalidToken);
        messages.put(SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING, invalidToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_SESSION_ID_MISSING, invalidToken);
        ApiErrorMessage internalError = new ApiErrorMessage("인증 처리 오류", "인증 처리 중 오류가 발생했습니다.");
        messages.put(SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION, internalError);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_REVOCATION_COUNT_INVALID, internalError);
        messages.put(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID, internalError);
        messages.put(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID, internalError);
        ApiErrorMessage invalidRefreshToken =
                new ApiErrorMessage("리프레시 토큰 검증 실패", "사용할 수 없는 리프레시 토큰입니다.");
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_EXPIRATION_MISMATCH, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_INACTIVE, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_SESSION_NOT_FOUND, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_SESSION_INACTIVE, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_ROLE_MISMATCH, invalidRefreshToken);
        messages.put(SecurityApplicationErrorCode.REFRESH_TOKEN_REPLAYED, invalidRefreshToken);
        return Map.copyOf(messages);
    }
}
