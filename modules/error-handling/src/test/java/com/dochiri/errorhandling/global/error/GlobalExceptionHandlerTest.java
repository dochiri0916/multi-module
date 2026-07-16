package com.dochiri.errorhandling.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String HTTP_GET = "GET";
    private static final String CODE_PROPERTY = "code";

    @Test
    @DisplayName("미처리 예외를 내부 메시지가 숨겨진 공통 500 응답으로 변환한다")
    void mapsUnhandledExceptionToCommonInternalServerError() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
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
    void addsCodeAndInstanceToSpringMvcProblemDetail() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
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
    void hidesCheckedExceptionMessageInCommonInternalServerError() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
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
    void mapsSpringMvcStatusToStableGlobalErrorCode() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
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
    void preservesExistingCommonCode() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
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

    @Test
    @DisplayName("제약 조건 위반 예외를 공통 검증 오류로 변환한다")
    void mapsConstraintViolationExceptionToValidationError() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/validate");
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        ConstraintDescriptor<NotBlank> descriptor = mock(ConstraintDescriptor.class);
        when(path.toString()).thenReturn("email");
        when(descriptor.getAnnotation()).thenReturn(notBlankAnnotation());
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        doReturn(descriptor).when(violation).getConstraintDescriptor();
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<Object> response = handler.handleConstraintViolationException(exception, request);

        // then
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getProperties()).containsEntry(
                ApiProblemDetailFactory.FIELD_ERRORS,
                List.of(new FieldErrorDetail("email", "must not be blank", "NotBlank"))
        );
    }

    @Test
    @DisplayName("필드가 없는 바인딩 오류를 객체 이름으로 변환한다")
    void mapsBindExceptionObjectErrorToObjectName() {
        // given
        ExposedGlobalExceptionHandler handler = new ExposedGlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/bind");
        BindException exception = new BindException(new Object(), "user");
        exception.addError(new ObjectError("user", "invalid user"));

        // when
        ResponseEntity<Object> response = handler.handleBindException(exception, request);

        // then
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getProperties()).containsEntry(
                ApiProblemDetailFactory.FIELD_ERRORS,
                List.of(new FieldErrorDetail("user", "invalid user", null))
        );
    }

    private static class ExposedGlobalExceptionHandler extends GlobalExceptionHandler {

        ExposedGlobalExceptionHandler() {
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

    private static NotBlank notBlankAnnotation() {
        return ValidationTarget.class.getDeclaredFields()[0].getAnnotation(NotBlank.class);
    }

    static class ValidationTarget {

        @NotBlank
        private String email;
    }

}
