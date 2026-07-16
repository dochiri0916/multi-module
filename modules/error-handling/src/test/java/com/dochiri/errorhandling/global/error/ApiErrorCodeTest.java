package com.dochiri.errorhandling.global.error;

import com.example.global.error.deep.NestedGlobalErrorCode;
import com.example.member.application.exception.MemberApplicationErrorCode;
import com.example.member.application.exception.MemberEdgeErrorCode;
import com.example.orphan.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeTest {

    @Test
    @DisplayName("Context 계층 오류 코드를 namespace가 포함된 API 코드로 변환한다")
    void mapsContextErrorCodeToNamespacedApiCode() {
        // given
        MemberApplicationErrorCode errorCode = MemberApplicationErrorCode.MEMBER_NOT_FOUND;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("MEMBER.NOT_FOUND");
    }

    @Test
    @DisplayName("전역 오류 코드를 GLOBAL namespace가 포함된 API 코드로 변환한다")
    void mapsGlobalErrorCodeToGlobalNamespacedApiCode() {
        // given
        GlobalErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("GLOBAL.INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("하위 global.error 패키지의 오류 코드를 GLOBAL namespace로 변환한다")
    void mapsNestedGlobalErrorCodeToGlobalNamespacedApiCode() {
        // given
        NestedGlobalErrorCode errorCode = NestedGlobalErrorCode.DEEP_FAILURE;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("GLOBAL.DEEP_FAILURE");
    }

    @Test
    @DisplayName("이름에서 오류 코드 접미사만 제거되면 GLOBAL namespace를 사용한다")
    void usesGlobalNamespaceWhenFallbackTypeNameIsOnlyErrorCode() {
        // given
        ErrorCode errorCode = ErrorCode.ONLY_ERROR_CODE;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("GLOBAL.ONLY_ERROR_CODE");
    }

    @Test
    @DisplayName("namespace 접두사만 있는 오류 코드 이름은 그대로 보존한다")
    void preservesNameWhenItOnlyStartsWithNamespaceSeparator() {
        // given
        MemberEdgeErrorCode errorCode = MemberEdgeErrorCode.MEMBER_;

        // when
        ApiErrorCode apiErrorCode = ApiErrorCode.from(errorCode);

        // then
        assertThat(apiErrorCode.value()).isEqualTo("MEMBER.MEMBER_");
    }

    @Test
    @DisplayName("API 오류 코드는 코드 값으로 동등성을 판단한다")
    void comparesCodesByValue() {
        // given
        ApiErrorCode first = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
        ApiErrorCode second = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);

        // when
        boolean equalCodes = first.equals(second);
        boolean sameCode = first.equals(first);
        boolean differentType = first.equals(new Object());
        boolean differentCode = first.equals(ApiErrorCode.from(GlobalErrorCode.BAD_REQUEST));

        // then
        assertThat(equalCodes).isTrue();
        assertThat(sameCode).isTrue();
        assertThat(differentType).isFalse();
        assertThat(differentCode).isFalse();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.toString()).isEqualTo(first.value());
    }
}
