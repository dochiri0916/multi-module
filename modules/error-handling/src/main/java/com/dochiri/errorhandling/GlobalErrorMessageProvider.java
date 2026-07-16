package com.dochiri.errorhandling;

import java.util.Map;

public final class GlobalErrorMessageProvider implements ApiErrorMessageProvider {

    private static final Map<ApiErrorCode, ApiErrorMessage> ERROR_MESSAGES = Map.of(
            ApiErrorCode.from(GlobalErrorCode.INTERNAL_SERVER_ERROR),
            new ApiErrorMessage("서버 오류", "일시적인 오류가 발생했습니다."),
            ApiErrorCode.from(GlobalErrorCode.VALIDATION_ERROR),
            new ApiErrorMessage("요청 검증 실패", "요청 값이 올바르지 않습니다."),
            ApiErrorCode.from(GlobalErrorCode.BAD_REQUEST),
            new ApiErrorMessage("잘못된 요청", "잘못된 요청입니다."),
            ApiErrorCode.from(GlobalErrorCode.NOT_FOUND),
            new ApiErrorMessage("요청 경로 없음", "요청한 리소스를 찾을 수 없습니다."),
            ApiErrorCode.from(GlobalErrorCode.METHOD_NOT_ALLOWED),
            new ApiErrorMessage("지원하지 않는 메서드", "지원하지 않는 HTTP 메서드입니다."),
            ApiErrorCode.from(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE),
            new ApiErrorMessage("지원하지 않는 미디어 타입", "지원하지 않는 Content-Type입니다.")
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMessage> errorMessages() {
        return Map.copyOf(ERROR_MESSAGES);
    }
}
