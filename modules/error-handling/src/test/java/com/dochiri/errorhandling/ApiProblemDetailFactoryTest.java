package com.dochiri.errorhandling;

import com.example.member.application.exception.MemberApplicationErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiProblemDetailFactoryTest {

    private static final String ERROR_TITLE = "회원 조회 실패";
    private static final String ERROR_DETAIL = "회원을 찾을 수 없습니다.";
    private static final String REQUEST_PATH = "/api/members/member-id";

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("매핑과 catalog 메시지로 namespace 오류 응답을 생성한다")
    void 매핑과_catalog_메시지로_namespace_오류_응답을_생성한다() {
        // given
        MDC.put("traceId", "trace-1");
        ApiProblemDetailFactory factory = new ApiProblemDetailFactory();
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorMapping mapping = new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND);
        ApiErrorMessage message = new ApiErrorMessage(ERROR_TITLE, ERROR_DETAIL);
        MappedApiError mappedError = new MappedApiError(
                code,
                mapping,
                Map.of("memberId", "member-id")
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);

        // when
        ProblemDetail body = factory.create(mappedError, message, new ServletWebRequest(request));

        // then
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getTitle()).isEqualTo(ERROR_TITLE);
        assertThat(body.getDetail()).isEqualTo(ERROR_DETAIL);
        assertThat(body.getType().toString()).isEqualTo("/problems/not-found");
        assertThat(body.getInstance().toString()).isEqualTo(REQUEST_PATH);
        assertThat(body.getProperties())
                .containsEntry("code", "MEMBER.NOT_FOUND")
                .containsEntry("traceId", "trace-1")
                .containsEntry("memberId", "member-id");
    }

    @Test
    @DisplayName("MDC traceId가 없으면 X-Request-Id를 오류 응답에 사용한다")
    void MDC_traceId가_없으면_X_Request_Id를_오류_응답에_사용한다() {
        // given
        ApiProblemDetailFactory factory = new ApiProblemDetailFactory();
        MappedApiError mappedError = mappedError(Map.of());
        ApiErrorMessage message = new ApiErrorMessage(ERROR_TITLE, ERROR_DETAIL);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
        request.addHeader("X-Request-Id", "request-1");

        // when
        ProblemDetail body = factory.create(mappedError, message, new ServletWebRequest(request));

        // then
        assertThat(body.getProperties()).containsEntry("traceId", "request-1");
    }

    @Test
    @DisplayName("Context provider도 예약된 오류 응답 속성을 덮어쓸 수 없다")
    void Context_provider도_예약된_오류_응답_속성을_덮어쓸_수_없다() {
        // given
        ApiProblemDetailFactory factory = new ApiProblemDetailFactory();
        MappedApiError mappedError = mappedError(Map.of("code", "OTHER.CODE"));
        ApiErrorMessage message = new ApiErrorMessage(ERROR_TITLE, ERROR_DETAIL);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);

        // when & then
        assertThatThrownBy(() -> factory.create(mappedError, message, new ServletWebRequest(request)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    private MappedApiError mappedError(Map<String, Object> properties) {
        return new MappedApiError(
                ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND),
                new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND),
                properties
        );
    }
}
