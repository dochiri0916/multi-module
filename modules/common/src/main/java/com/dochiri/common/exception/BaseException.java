package com.dochiri.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class BaseException extends ErrorResponseException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public BaseException(ErrorCode errorCode, Map<String, Object> properties) {
        super(requireErrorCode(errorCode).getHttpStatus(), createBody(errorCode), null);
        this.errorCode = errorCode;

        getBody().setProperty("code", errorCode.name());

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            getBody().setProperty(entry.getKey(), entry.getValue());
        }
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static BaseException of(ErrorCode errorCode, Object... keyValues) {
        return new BaseException(errorCode, mapArgs(keyValues));
    }

    private static ProblemDetail createBody(ErrorCode errorCode) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), errorCode.getMessage());
        body.setType(URI.create("/errors/" + toKebabCase(errorCode.name())));
        body.setTitle(errorCode.name());
        return body;
    }

    private static ErrorCode requireErrorCode(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
    }

    private static String toKebabCase(String name) {
        return name.toLowerCase().replace('_', '-');
    }

    private static Map<String, Object> mapArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return Map.of();
        }

        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("args는 키/값 쌍이어야 합니다.");
        }

        Map<String, Object> mapped = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index += 2) {
            String key = String.valueOf(args[index]);
            Object value = args[index + 1];
            mapped.put(key, value);
        }
        return Map.copyOf(mapped);
    }
}
