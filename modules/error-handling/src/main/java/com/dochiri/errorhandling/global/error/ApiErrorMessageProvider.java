package com.dochiri.errorhandling.global.error;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ApiErrorMessageProvider {

    Map<ApiErrorCode, ApiErrorMessage> errorMessages();

    default Optional<ApiErrorMessage> findByCode(ApiErrorCode errorCode) {
        ApiErrorCode code = Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
        return Optional.ofNullable(errorMessages().get(code));
    }
}
