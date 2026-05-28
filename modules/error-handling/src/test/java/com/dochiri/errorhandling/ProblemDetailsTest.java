package com.dochiri.errorhandling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void ErrorCode로_표준_ProblemDetail을_생성한다() {
        ProblemDetail body = ProblemDetails.from(CommonErrorCode.BAD_REQUEST);

        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("BAD_REQUEST");
        assertThat(body.getDetail()).isEqualTo("잘못된 요청입니다.");
        assertThat(body.getType().toString()).isEqualTo("/errors/bad-request");
        assertThat(body.getProperties()).containsEntry("code", "BAD_REQUEST");
    }

    @Test
    void request_uri와_traceId를_보강한다() {
        MDC.put("traceId", "trace-1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");

        ProblemDetail body = ProblemDetails.internalServerError(new ServletWebRequest(request));

        assertThat(body.getInstance().toString()).isEqualTo("/api/users/1");
        assertThat(body.getProperties()).containsEntry("traceId", "trace-1");
    }

    @Test
    void MDC_traceId가_없으면_X_Request_Id를_traceId로_사용한다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader("X-Request-Id", "request-1");

        ProblemDetail body = ProblemDetails.internalServerError(new ServletWebRequest(request));

        assertThat(body.getProperties()).containsEntry("traceId", "request-1");
    }

    @Test
    void validation_응답에는_fieldErrors가_포함된다() {
        FieldErrorDetail fieldError = new FieldErrorDetail("email", "abc", "올바른 이메일 형식이어야 합니다.", "Email");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");

        ProblemDetail body = ProblemDetails.validationError(java.util.List.of(fieldError), new ServletWebRequest(request));

        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getProperties())
                .containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("fieldErrors", java.util.List.of(fieldError));
    }

}
