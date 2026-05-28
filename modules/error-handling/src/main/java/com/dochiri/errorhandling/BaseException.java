package com.dochiri.errorhandling;

import lombok.Getter;
import org.springframework.web.ErrorResponseException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class BaseException extends ErrorResponseException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public BaseException(ErrorCode errorCode, Map<String, Object> properties) {
        this(errorCode, properties, null);
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Map.of(), cause);
    }

    public BaseException(ErrorCode errorCode, Map<String, Object> properties, Throwable cause) {
        super(requireErrorCode(errorCode).getStatusCode(), ProblemDetails.from(errorCode, requireProperties(properties)), cause);
        this.errorCode = errorCode;
    }

    public static BaseException of(ErrorCode errorCode, Object... keyValues) {
        return new BaseException(errorCode, mapArgs(keyValues));
    }

    private static ErrorCode requireErrorCode(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");
    }

    private static Map<String, Object> requireProperties(Map<String, Object> properties) {
        return Objects.requireNonNull(properties, "properties는 필수입니다.");
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
            Object rawKey = args[index];
            if (!(rawKey instanceof String key)) {
                throw new IllegalArgumentException("property key는 String이어야 합니다.");
            }
            Object value = args[index + 1];
            mapped.put(key, value);
        }
        return Collections.unmodifiableMap(mapped);
    }

}
