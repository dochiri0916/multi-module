package com.dochiri.errorhandling.global.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ApiProblemDetailFactory {

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

    public ProblemDetail create(
            MappedApiError mappedError,
            ApiErrorMessage message,
            WebRequest request
    ) {
        MappedApiError error = Objects.requireNonNull(mappedError, "mappedError는 필수입니다.");
        ApiErrorMessage errorMessage = Objects.requireNonNull(message, "message는 필수입니다.");
        Objects.requireNonNull(request, "request는 필수입니다.");

        ProblemDetail body = ProblemDetail.forStatusAndDetail(error.mapping().status(), errorMessage.detail());
        body.setType(typeOf(error.mapping().problemType()));
        body.setTitle(errorMessage.title());
        body.setProperty(CODE, error.code().value());
        addPublicProperties(body, error.properties());
        applyRequestDetails(body, request);
        return body;
    }

    public ProblemDetail createValidation(
            MappedApiError mappedError,
            ApiErrorMessage message,
            Collection<FieldErrorDetail> fieldErrors,
            WebRequest request
    ) {
        Objects.requireNonNull(fieldErrors, "fieldErrors는 필수입니다.");
        ProblemDetail body = create(mappedError, message, request);
        body.setProperty(FIELD_ERRORS, List.copyOf(fieldErrors));
        return body;
    }

    private void addPublicProperties(ProblemDetail body, Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = validatePropertyKey(entry.getKey());
            body.setProperty(key, entry.getValue());
        }
    }

    private void applyRequestDetails(ProblemDetail body, WebRequest request) {
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

    private String validatePropertyKey(String key) {
        if (!hasText(key)) {
            throw new IllegalArgumentException("property key는 비어 있을 수 없습니다.");
        }
        if (RESERVED_PROPERTIES.contains(key)) {
            throw new IllegalArgumentException("예약된 property key는 사용할 수 없습니다: " + key);
        }
        return key;
    }

    private URI typeOf(ProblemType problemType) {
        String slug = problemType.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return URI.create("/problems/" + slug);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
