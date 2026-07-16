package com.dochiri.errorhandling;

import com.example.member.application.exception.MemberApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeTest {

    @Test
    @DisplayName("Context 계층 오류 코드를 namespace가 포함된 API 코드로 변환한다")
    void Context_계층_오류_코드를_namespace가_포함된_API_코드로_변환한다() {
        // given
        MemberApplicationErrorCode errorCode = MemberApplicationErrorCode.MEMBER_NOT_FOUND;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("MEMBER.NOT_FOUND");
    }

    @Test
    @DisplayName("전역 오류 코드를 GLOBAL namespace가 포함된 API 코드로 변환한다")
    void 전역_오류_코드를_GLOBAL_namespace가_포함된_API_코드로_변환한다() {
        // given
        GlobalErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("GLOBAL.INTERNAL_SERVER_ERROR");
    }
}
