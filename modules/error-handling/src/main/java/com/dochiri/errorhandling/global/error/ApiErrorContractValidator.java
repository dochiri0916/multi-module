package com.dochiri.errorhandling.global.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApiErrorContractValidator {

    private final List<ErrorCodeMappingProvider> mappingProviders;
    private final List<ApiErrorMessageProvider> messageProviders;

    public ApiErrorContractValidator(
            List<ErrorCodeMappingProvider> mappingProviders,
            List<ApiErrorMessageProvider> messageProviders
    ) {
        this.mappingProviders = List.copyOf(Objects.requireNonNull(mappingProviders, "mappingProviders는 필수입니다."));
        this.messageProviders = List.copyOf(Objects.requireNonNull(messageProviders, "messageProviders는 필수입니다."));
    }

    public Set<ApiErrorCode> validate() {
        Map<ApiErrorCode, Integer> mappingOccurrences = occurrences(mappingProviders.stream()
                .map(ErrorCodeMappingProvider::errorCodeMappings)
                .toList());
        Map<ApiErrorCode, Integer> messageOccurrences = occurrences(messageProviders.stream()
                .map(ApiErrorMessageProvider::errorMessages)
                .toList());

        validateUnique("API 오류 매핑", mappingOccurrences);
        validateUnique("API 오류 메시지", messageOccurrences);

        Set<ApiErrorCode> mappingCodes = mappingOccurrences.keySet();
        Set<ApiErrorCode> messageCodes = messageOccurrences.keySet();
        if (!mappingCodes.equals(messageCodes)) {
            throw new IllegalStateException("API 오류 매핑과 메시지 카탈로그가 일치하지 않습니다: mapping="
                    + mappingCodes + ", messages=" + messageCodes);
        }
        return Set.copyOf(mappingCodes);
    }

    private Map<ApiErrorCode, Integer> occurrences(List<? extends Map<ApiErrorCode, ?>> entries) {
        Map<ApiErrorCode, Integer> occurrences = new HashMap<>();
        for (Map<ApiErrorCode, ?> entry : entries) {
            for (ApiErrorCode code : entry.keySet()) {
                occurrences.merge(code, 1, Integer::sum);
            }
        }
        return Map.copyOf(occurrences);
    }

    private void validateUnique(String contractName, Map<ApiErrorCode, Integer> occurrences) {
        Set<String> duplicateCodes = occurrences.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .map(ApiErrorCode::value)
                .collect(Collectors.toUnmodifiableSet());
        if (!duplicateCodes.isEmpty()) {
            throw new IllegalStateException(contractName + "에 중복 코드가 있습니다: " + duplicateCodes);
        }
    }
}
