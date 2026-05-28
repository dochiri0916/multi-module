package com.dochiri.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProblemDetails {

    public static final String CODE = "code";
    public static final String TRACE_ID = "traceId";
    public static final String FIELD_ERRORS = "fieldErrors";

    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final Set<String> RESERVED_PROPERTIES = Set.of(
            "type",
            "title",
            "status",
            "detail",
            "instance",
            CODE,
            TRACE_ID,
            FIELD_ERRORS,
            "errors",
            "timestamp",
            "path",
            "exception",
            "message"
    );

    private ProblemDetails() {
    }

    public static ProblemDetail from(ErrorCode errorCode) {
        return from(errorCode, Map.of());
    }

    public static ProblemDetail from(ErrorCode errorCode, Map<String, Object> properties) {
        Objects.requireNonNull(errorCode, "errorCode는 필수입니다.");

        ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.getStatusCode(), errorCode.getMessage());
        body.setType(typeOf(errorCode.name()));
        body.setTitle(errorCode.name());
        body.setProperty(CODE, errorCode.name());
        addProperties(body, properties);
        return body;
    }

    public static ProblemDetail internalServerError(WebRequest request) {
        ProblemDetail body = from(CommonErrorCode.INTERNAL_SERVER_ERROR);
        applyRequestDetails(body, request);
        return body;
    }

    public static ProblemDetail validationError(Collection<FieldErrorDetail> fieldErrors, WebRequest request) {
        Objects.requireNonNull(fieldErrors, "fieldErrors는 필수입니다.");

        ProblemDetail body = from(CommonErrorCode.VALIDATION_ERROR);
        body.setProperty(FIELD_ERRORS, fieldErrors);
        applyRequestDetails(body, request);
        return body;
    }

    public static ProblemDetail normalize(ProblemDetail body, HttpStatusCode statusCode, WebRequest request) {
        Objects.requireNonNull(body, "body는 필수입니다.");
        Objects.requireNonNull(statusCode, "statusCode는 필수입니다.");

        if (body.getStatus() == 0) {
            body.setStatus(statusCode.value());
        }

        applyCommonCode(body, statusCode);
        applyRequestDetails(body, request);
        return body;
    }

    public static void addProperties(ProblemDetail body, Map<String, Object> properties) {
        Objects.requireNonNull(body, "body는 필수입니다.");
        Objects.requireNonNull(properties, "properties는 필수입니다.");

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = validatePropertyKey(entry.getKey());
            body.setProperty(key, entry.getValue());
        }
    }

    private static void applyCommonCode(ProblemDetail body, HttpStatusCode statusCode) {
        if (body.getProperties() != null && body.getProperties().containsKey(CODE)) {
            return;
        }

        CommonErrorCode commonErrorCode = commonErrorCodeOf(statusCode);
        body.setProperty(CODE, commonErrorCode.name());
        if (body.getType() == null || URI.create("about:blank").equals(body.getType())) {
            body.setType(typeOf(commonErrorCode.name()));
        }
        if (body.getTitle() == null || HttpStatus.valueOf(statusCode.value()).getReasonPhrase().equals(body.getTitle())) {
            body.setTitle(commonErrorCode.name());
        }
    }

    private static CommonErrorCode commonErrorCodeOf(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> CommonErrorCode.BAD_REQUEST;
            case 404 -> CommonErrorCode.NOT_FOUND;
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> statusCode.is5xxServerError()
                    ? CommonErrorCode.INTERNAL_SERVER_ERROR
                    : CommonErrorCode.BAD_REQUEST;
        };
    }

    private static void applyRequestDetails(ProblemDetail body, WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            body.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }

        String traceId = MDC.get("traceId");
        if (!hasText(traceId)) {
            traceId = MDC.get("trace_id");
        }
        if (hasText(traceId)) {
            body.setProperty(TRACE_ID, traceId);
            return;
        }

        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest servletRequest = servletWebRequest.getRequest();
            String requestId = servletRequest.getHeader(X_REQUEST_ID);
            if (hasText(requestId)) {
                body.setProperty(TRACE_ID, requestId);
            }
        }
    }

    private static String validatePropertyKey(String key) {
        if (!hasText(key)) {
            throw new IllegalArgumentException("property key는 비어 있을 수 없습니다.");
        }
        if (RESERVED_PROPERTIES.contains(key)) {
            throw new IllegalArgumentException("예약된 property key는 사용할 수 없습니다: " + key);
        }
        return key;
    }

    private static URI typeOf(String code) {
        return URI.create("/errors/" + code.toLowerCase().replace('_', '-'));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
