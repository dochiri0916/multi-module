package com.dochiri.errorhandling;

import java.util.List;
import java.util.Objects;

public final class ApiErrorMessageCatalog {

    private static final int SINGLE_MESSAGE = 1;

    private final List<ApiErrorMessageProvider> providers;

    public ApiErrorMessageCatalog(List<ApiErrorMessageProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers는 필수입니다."));
    }

    public ApiErrorMessage messageFor(ApiErrorCode errorCode) {
        ApiErrorCode code = Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
        List<ApiErrorMessage> messages = providers.stream()
                .map(provider -> provider.findByCode(code))
                .flatMap(java.util.Optional::stream)
                .toList();
        if (messages.isEmpty()) {
            throw new IllegalStateException("API 오류 메시지를 찾을 수 없습니다: " + code.value());
        }
        if (messages.size() > SINGLE_MESSAGE) {
            throw new IllegalStateException("API 오류 메시지가 중복되었습니다: " + code.value());
        }
        return messages.getFirst();
    }
}
