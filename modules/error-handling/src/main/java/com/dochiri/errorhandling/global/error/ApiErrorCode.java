package com.dochiri.errorhandling.global.error;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ApiErrorCode {

    private static final String SEPARATOR = ".";
    private static final Set<String> LAYER_SEGMENTS = Set.of("domain", "application", "adapter");

    private final String code;

    private ApiErrorCode(String code) {
        this.code = code;
    }

    public static ApiErrorCode from(Enum<?> errorCode) {
        Enum<?> code = Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
        String namespace = namespace(code.getDeclaringClass());
        return new ApiErrorCode(namespace + SEPARATOR + normalizedName(namespace, code.name()));
    }

    public String value() {
        return code;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof ApiErrorCode that && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    private static String namespace(Class<?> errorCodeType) {
        String packageName = errorCodeType.getPackageName();
        if (packageName.endsWith(".global.error") || packageName.contains(".global.error.")) {
            return "GLOBAL";
        }

        String[] segments = packageName.split("\\.");
        for (int index = 1; index < segments.length; index++) {
            if (LAYER_SEGMENTS.contains(segments[index])) {
                return segments[index - 1].toUpperCase(Locale.ROOT);
            }
        }
        return fallbackNamespace(errorCodeType);
    }

    private static String fallbackNamespace(Class<?> errorCodeType) {
        String simpleName = errorCodeType.getSimpleName()
                .replace("DomainErrorCode", "")
                .replace("ApplicationErrorCode", "")
                .replace("ErrorCode", "");
        if (simpleName.isBlank()) {
            return "GLOBAL";
        }
        return simpleName.toUpperCase(Locale.ROOT);
    }

    private static String normalizedName(String namespace, String name) {
        String duplicatedNamespace = namespace + "_";
        if (name.startsWith(duplicatedNamespace) && name.length() > duplicatedNamespace.length()) {
            return name.substring(duplicatedNamespace.length());
        }
        return name;
    }
}
