package com.dochiri.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String HTTP_GET = "GET";
    private static final String CODE_PROPERTY = "code";

    @Test
    @DisplayName("미처리 예외를 내부 메시지가 숨겨진 공통 500 응답으로 변환한다")
    void 미처리_예외는_공통_500_응답으로_변환한다() {
        // given
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/fail");
        request.addHeader("X-Request-Id", "request-1");

        // when
        ResponseEntity<Object> response = handler.handleUncaughtException(new RuntimeException("노출되면 안 됨"), request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body.getTitle()).isEqualTo("서버 오류");
        assertThat(body.getDetail()).isEqualTo("일시적인 오류가 발생했습니다.");
        assertThat(body.getProperties())
                .containsEntry(CODE_PROPERTY, "GLOBAL.INTERNAL_SERVER_ERROR")
                .containsEntry("traceId", "request-1");
    }

    @Test
    @DisplayName("Spring MVC ProblemDetail에 공통 code와 요청 URI를 추가한다")
    void Spring_MVC_ProblemDetail_응답에_code와_instance를_보강한다() {
        // given
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "not found");
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/missing");

        // when
        ResponseEntity<Object> response = handler.exposeHandleExceptionInternal(
                new RuntimeException("not found"),
                body,
                new HttpHeaders(),
                HttpStatus.NOT_FOUND,
                request
        );

        // then
        ProblemDetail responseBody = (ProblemDetail) response.getBody();
        assertThat(responseBody.getTitle()).isEqualTo("요청 경로 없음");
        assertThat(responseBody.getDetail()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
        assertThat(responseBody.getInstance().toString()).isEqualTo("/missing");
        assertThat(responseBody.getProperties()).containsEntry(CODE_PROPERTY, "GLOBAL.NOT_FOUND");
    }

    @Test
    @DisplayName("checked 예외도 내부 메시지를 숨긴 공통 500 응답으로 변환한다")
    void checked_예외도_내부_메시지를_숨긴_공통_500_응답으로_변환한다() {
        // given
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/checked-fail");

        // when
        ResponseEntity<Object> response = handler.handleUncaughtException(new Exception("internal"), request);

        // then
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getDetail()).isEqualTo("일시적인 오류가 발생했습니다.");
        assertThat(body.getProperties()).containsEntry(CODE_PROPERTY, "GLOBAL.INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Spring MVC 상태를 안정적인 전역 오류 코드로 변환한다")
    void Spring_MVC_상태를_안정적인_전역_오류_코드로_변환한다() {
        // given
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/status");
        List<HttpStatus> statuses = List.of(
                HttpStatus.BAD_REQUEST,
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        // when
        List<String> codes = statuses.stream()
                .map(status -> handler.exposeHandleExceptionInternal(
                        new RuntimeException("internal"),
                        null,
                        new HttpHeaders(),
                        status,
                        request
                ))
                .map(response -> (ProblemDetail) response.getBody())
                .map(body -> Objects.requireNonNull(body.getProperties()).get(CODE_PROPERTY))
                .map(String.class::cast)
                .toList();

        // then
        assertThat(codes).containsExactly(
                "GLOBAL.BAD_REQUEST",
                "GLOBAL.METHOD_NOT_ALLOWED",
                "GLOBAL.UNSUPPORTED_MEDIA_TYPE",
                "GLOBAL.INTERNAL_SERVER_ERROR"
        );
    }

    @Test
    @DisplayName("이미 공통 code가 있는 ProblemDetail은 교체하지 않는다")
    void 이미_공통_code가_있는_ProblemDetail은_교체하지_않는다() {
        // given
        TestGlobalExceptionHandler handler = new TestGlobalExceptionHandler();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "adapter detail");
        body.setProperty(CODE_PROPERTY, "MEMBER.ADAPTER_FAILURE");
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/member");

        // when
        ResponseEntity<Object> response = handler.exposeHandleExceptionInternal(
                new RuntimeException("internal"),
                body,
                new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );

        // then
        assertThat(response.getBody()).isSameAs(body);
        assertThat(((ProblemDetail) response.getBody()).getProperties())
                .containsEntry(CODE_PROPERTY, "MEMBER.ADAPTER_FAILURE");
    }

    private static class TestGlobalExceptionHandler extends GlobalExceptionHandler {

        TestGlobalExceptionHandler() {
            super(
                    new ApiProblemDetailFactory(),
                    new ApiExceptionMapper(List.of(new GlobalErrorCodeMappingProvider())),
                    new ApiErrorMessageCatalog(List.of(new GlobalErrorMessageProvider()))
            );
        }

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
