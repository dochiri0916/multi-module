package com.dochiri.errorhandling.global.error;

import com.example.member.application.exception.MemberApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiErrorContractValidatorTest {

    @Test
    @DisplayName("모든 API 코드에 매핑과 메시지가 하나씩 있으면 계약 검증에 성공한다")
    void validatesCompleteApiErrorContract() {
        // given
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorContractValidator validator = new ApiErrorContractValidator(
                List.of(mappingProvider(code)),
                List.of(messageProvider(code))
        );

        // when
        Set<ApiErrorCode> validatedCodes = validator.validate();

        // then
        assertThat(validatedCodes).containsExactly(code);
    }

    @Test
    @DisplayName("같은 API 코드의 HTTP 매핑이 둘 이상이면 시작 계약 검증에 실패한다")
    void rejectsDuplicateHttpMappingForApiCode() {
        // given
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorContractValidator validator = new ApiErrorContractValidator(
                List.of(mappingProvider(code), mappingProvider(code)),
                List.of(messageProvider(code))
        );

        // when & then
        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API 오류 매핑")
                .hasMessageContaining(code.value());
    }

    @Test
    @DisplayName("같은 API 코드의 사용자 메시지가 둘 이상이면 시작 계약 검증에 실패한다")
    void rejectsDuplicateMessageForApiCode() {
        // given
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorContractValidator validator = new ApiErrorContractValidator(
                List.of(mappingProvider(code)),
                List.of(messageProvider(code), messageProvider(code))
        );

        // when & then
        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API 오류 메시지")
                .hasMessageContaining(code.value());
    }

    @Test
    @DisplayName("HTTP 매핑과 사용자 메시지의 API 코드 집합이 다르면 시작 계약 검증에 실패한다")
    void rejectsMismatchedApiCodeSets() {
        // given
        ApiErrorCode mappingCode = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorCode messageCode = ApiErrorCode.from(GlobalErrorCode.BAD_REQUEST);
        ApiErrorContractValidator validator = new ApiErrorContractValidator(
                List.of(mappingProvider(mappingCode)),
                List.of(messageProvider(messageCode))
        );

        // when & then
        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않습니다")
                .hasMessageContaining(mappingCode.value())
                .hasMessageContaining(messageCode.value());
    }

    private ErrorCodeMappingProvider mappingProvider(ApiErrorCode code) {
        return () -> Map.of(
                code,
                new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND)
        );
    }

    private ApiErrorMessageProvider messageProvider(ApiErrorCode code) {
        return () -> Map.of(
                code,
                new ApiErrorMessage("회원 조회 실패", "회원을 찾을 수 없습니다.")
        );
    }
}
