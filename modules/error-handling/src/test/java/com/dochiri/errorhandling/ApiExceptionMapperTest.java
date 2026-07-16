package com.dochiri.errorhandling;

import com.example.member.adapter.in.web.MemberErrorCodeMappingProvider;
import com.example.member.application.exception.MemberNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionMapperTest {

    @Test
    @DisplayName("Context provider가 plain Application 예외를 API 오류 매핑으로 변환한다")
    void Context_provider가_plain_Application_예외를_API_오류_매핑으로_변환한다() {
        // given
        ApiExceptionMapper mapper = new ApiExceptionMapper(List.of(new MemberErrorCodeMappingProvider()));
        MemberNotFoundException exception = MemberNotFoundException.memberNotFound("member-id");

        // when
        Optional<MappedApiError> mappedError = mapper.map(exception);

        // then
        assertThat(mappedError).hasValueSatisfying(error -> {
            assertThat(error.code().value()).isEqualTo("MEMBER.NOT_FOUND");
            assertThat(error.mapping().status()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.mapping().problemType()).isEqualTo(ProblemType.NOT_FOUND);
            assertThat(error.properties()).containsEntry("memberId", "member-id");
        });
    }

    @Test
    @DisplayName("등록된 Context provider가 모르는 예외는 매핑하지 않는다")
    void 등록된_Context_provider가_모르는_예외는_매핑하지_않는다() {
        // given
        ApiExceptionMapper mapper = new ApiExceptionMapper(List.of(new MemberErrorCodeMappingProvider()));
        RuntimeException exception = new IllegalStateException("internal");

        // when
        Optional<MappedApiError> mappedError = mapper.map(exception);

        // then
        assertThat(mappedError).isEmpty();
    }
}
