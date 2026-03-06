package com.dochiri.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseExceptionTest {

    enum TestErrorCode implements ErrorCode {
        TEST_ERROR(HttpStatus.BAD_REQUEST, "테스트 에러입니다."),
        SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다.");

        private final HttpStatus httpStatus;
        private final String message;

        TestErrorCode(HttpStatus httpStatus, String message) {
            this.httpStatus = httpStatus;
            this.message = message;
        }

        @Override
        public HttpStatus getHttpStatus() { return httpStatus; }

        @Override
        public String getMessage() { return message; }
    }

    @Test
    void ErrorCode만으로_생성하면_ProblemDetail이_올바르게_구성된다() {
        BaseException exception = new BaseException(TestErrorCode.TEST_ERROR);

        ProblemDetail body = exception.getBody();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getDetail()).isEqualTo("테스트 에러입니다.");
        assertThat(body.getTitle()).isEqualTo("TEST_ERROR");
        assertThat(body.getType().toString()).isEqualTo("/errors/test-error");
        assertThat(body.getProperties()).containsEntry("code", "TEST_ERROR");
    }

    @Test
    void 추가_속성을_Map으로_전달하면_ProblemDetail에_포함된다() {
        Map<String, Object> properties = Map.of("userId", 123L, "field", "email");
        BaseException exception = new BaseException(TestErrorCode.TEST_ERROR, properties);

        ProblemDetail body = exception.getBody();
        assertThat(body.getProperties())
                .containsEntry("code", "TEST_ERROR")
                .containsEntry("userId", 123L)
                .containsEntry("field", "email");
    }

    @Test
    void of_팩토리_메서드로_키값_쌍을_전달할_수_있다() {
        BaseException exception = BaseException.of(TestErrorCode.TEST_ERROR, "userId", 42L);

        ProblemDetail body = exception.getBody();
        assertThat(body.getProperties())
                .containsEntry("code", "TEST_ERROR")
                .containsEntry("userId", 42L);
    }

    @Test
    void of_팩토리_메서드에_홀수_인자를_전달하면_예외가_발생한다() {
        assertThatThrownBy(() -> BaseException.of(TestErrorCode.TEST_ERROR, "key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ErrorCode가_null이면_NullPointerException이_발생한다() {
        assertThatThrownBy(() -> new BaseException(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getErrorCode로_원본_ErrorCode를_조회할_수_있다() {
        BaseException exception = new BaseException(TestErrorCode.SERVER_ERROR);

        assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.SERVER_ERROR);
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void 빈_속성으로_생성하면_code_속성만_존재한다() {
        BaseException exception = new BaseException(TestErrorCode.TEST_ERROR, Map.of());

        assertThat(exception.getBody().getProperties()).containsOnlyKeys("code");
    }
}
