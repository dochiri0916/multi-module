package com.dochiri.errorhandling.global.error;

import com.example.member.adapter.in.web.MemberErrorCodeMappingProvider;
import com.example.member.application.exception.MemberApplicationErrorCode;
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
    void mapsPlainApplicationExceptionFromContextProvider() {
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
    void returnsEmptyForUnknownException() {
        // given
        ApiExceptionMapper mapper = new ApiExceptionMapper(List.of(new MemberErrorCodeMappingProvider()));
        RuntimeException exception = new IllegalStateException("internal");

        // when
        Optional<MappedApiError> mappedError = mapper.map(exception);

        // then
        assertThat(mappedError).isEmpty();
    }

    @Test
    @DisplayName("매핑 provider는 등록된 코드만 오류 매핑으로 변환한다")
    void resolvesRegisteredCodeAndReturnsEmptyForUnknownCode() {
        // given
        ErrorCodeMappingProvider provider = new MemberErrorCodeMappingProvider();
        ApiErrorCode registeredCode = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorCode unknownCode = ApiErrorCode.from(GlobalErrorCode.BAD_REQUEST);

        // when
        Optional<MappedApiError> mappedError = provider.resolve(registeredCode);
        Optional<MappedApiError> unknownError = provider.resolve(unknownCode);

        // then
        assertThat(mappedError).isPresent();
        assertThat(mappedError.orElseThrow().code()).isEqualTo(registeredCode);
        assertThat(unknownError).isEmpty();
    }
}
