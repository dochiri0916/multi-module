package com.dochiri.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void 미처리_예외는_공통_500_응답으로_변환한다() {
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/fail");
        request.addHeader("X-Request-Id", "request-1");

        ResponseEntity<Object> response = handler.handleUncaughtException(new RuntimeException("노출되면 안 됨"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body.getDetail()).isEqualTo("일시적인 오류가 발생했습니다.");
        assertThat(body.getProperties())
                .containsEntry("code", "INTERNAL_SERVER_ERROR")
                .containsEntry("traceId", "request-1");
    }

    @Test
    void Spring_MVC_ProblemDetail_응답에_code와_instance를_보강한다() {
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "not found");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");

        ResponseEntity<Object> response = handler.exposeHandleExceptionInternal(
                new RuntimeException("not found"),
                body,
                new HttpHeaders(),
                HttpStatus.NOT_FOUND,
                request
        );

        ProblemDetail responseBody = (ProblemDetail) response.getBody();
        assertThat(responseBody.getInstance().toString()).isEqualTo("/missing");
        assertThat(responseBody.getProperties()).containsEntry("code", "NOT_FOUND");
    }

    private static class TestGlobalExceptionHandler extends GlobalExceptionHandler {

        ResponseEntity<Object> exposeHandleExceptionInternal(
                Exception exception,
                Object body,
                HttpHeaders headers,
                HttpStatus status,
                HttpServletRequest request
        ) {
            return handleExceptionInternal(exception, body, headers, status, new ServletWebRequest(request));
        }

    }

}
