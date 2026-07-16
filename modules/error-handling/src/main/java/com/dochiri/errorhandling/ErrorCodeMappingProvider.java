package com.dochiri.errorhandling;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ErrorCodeMappingProvider {

    Map<ApiErrorCode, ApiErrorMapping> errorCodeMappings();

    default Optional<MappedApiError> resolve(RuntimeException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        return Optional.empty();
    }

    default Optional<MappedApiError> resolve(Enum<?> errorCode) {
        return resolve(ApiErrorCode.from(errorCode));
    }

    default Optional<MappedApiError> resolve(ApiErrorCode errorCode) {
        ApiErrorCode code = Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
        ApiErrorMapping mapping = errorCodeMappings().get(code);
        if (mapping == null) {
            return Optional.empty();
        }
        return Optional.of(MappedApiError.from(code, mapping));
    }
}
