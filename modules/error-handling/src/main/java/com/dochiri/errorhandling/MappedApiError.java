package com.dochiri.errorhandling;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record MappedApiError(
        ApiErrorCode code,
        ApiErrorMapping mapping,
        Map<String, Object> properties
) {

    public MappedApiError {
        Objects.requireNonNull(code, "code는 필수입니다.");
        Objects.requireNonNull(mapping, "mapping은 필수입니다.");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties는 필수입니다."));
    }

    public static MappedApiError from(ApiErrorCode code, ApiErrorMapping mapping) {
        return new MappedApiError(code, mapping, Map.of());
    }

    public MappedApiError withProperties(Map<String, Object> additionalProperties) {
        Map<String, Object> mergedProperties = new HashMap<>(properties);
        mergedProperties.putAll(Objects.requireNonNull(additionalProperties, "additionalProperties는 필수입니다."));
        return new MappedApiError(code, mapping, mergedProperties);
    }
}
