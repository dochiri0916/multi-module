package com.dochiri.errorhandling.global.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public final class GlobalErrorCodeMappingProvider implements ErrorCodeMappingProvider {

    private static final Map<ApiErrorCode, ApiErrorMapping> ERROR_CODE_MAPPINGS = Map.of(
            ApiErrorCode.from(GlobalErrorCode.INTERNAL_SERVER_ERROR),
            new ApiErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, ProblemType.INTERNAL_ERROR),
            ApiErrorCode.from(GlobalErrorCode.VALIDATION_ERROR),
            new ApiErrorMapping(HttpStatus.BAD_REQUEST, ProblemType.VALIDATION_FAILED),
            ApiErrorCode.from(GlobalErrorCode.BAD_REQUEST),
            new ApiErrorMapping(HttpStatus.BAD_REQUEST, ProblemType.BAD_REQUEST),
            ApiErrorCode.from(GlobalErrorCode.NOT_FOUND),
            new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND),
            ApiErrorCode.from(GlobalErrorCode.METHOD_NOT_ALLOWED),
            new ApiErrorMapping(HttpStatus.METHOD_NOT_ALLOWED, ProblemType.METHOD_NOT_ALLOWED),
            ApiErrorCode.from(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE),
            new ApiErrorMapping(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ProblemType.UNSUPPORTED_MEDIA_TYPE)
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMapping> errorCodeMappings() {
        return Map.copyOf(ERROR_CODE_MAPPINGS);
    }
}
