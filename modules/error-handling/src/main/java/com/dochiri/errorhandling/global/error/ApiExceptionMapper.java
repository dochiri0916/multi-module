package com.dochiri.errorhandling.global.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ApiExceptionMapper {

    private static final int SINGLE_MAPPING = 1;

    private final List<ErrorCodeMappingProvider> providers;

    public ApiExceptionMapper(List<ErrorCodeMappingProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers는 필수입니다."));
    }

    public Optional<MappedApiError> map(RuntimeException exception) {
        Objects.requireNonNull(exception, "exception은 필수입니다.");
        List<MappedApiError> resolvedErrors = new ArrayList<>();
        for (ErrorCodeMappingProvider provider : providers) {
            provider.resolve(exception).ifPresent(resolvedError -> {
                validateProviderMapping(provider, resolvedError);
                resolvedErrors.add(resolvedError);
            });
        }
        if (resolvedErrors.size() > SINGLE_MAPPING) {
            throw new IllegalStateException("하나의 예외가 여러 API 오류로 매핑되었습니다: "
                    + exception.getClass().getName());
        }
        return resolvedErrors.stream().findFirst();
    }

    public ApiErrorMapping mappingFor(ApiErrorCode errorCode) {
        ApiErrorCode code = Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
        List<ApiErrorMapping> mappings = providers.stream()
                .map(ErrorCodeMappingProvider::errorCodeMappings)
                .map(entries -> entries.get(code))
                .filter(Objects::nonNull)
                .toList();
        if (mappings.isEmpty()) {
            throw new IllegalStateException("API 오류 매핑을 찾을 수 없습니다: " + code.value());
        }
        if (mappings.size() > SINGLE_MAPPING) {
            throw new IllegalStateException("API 오류 매핑이 중복되었습니다: " + code.value());
        }
        return mappings.getFirst();
    }

    private void validateProviderMapping(ErrorCodeMappingProvider provider, MappedApiError resolvedError) {
        ApiErrorMapping registeredMapping = provider.errorCodeMappings().get(resolvedError.code());
        if (!resolvedError.mapping().equals(registeredMapping)) {
            throw new IllegalStateException("Provider가 등록하지 않은 API 오류 매핑을 반환했습니다: "
                    + resolvedError.code().value());
        }
    }
}
