package com.dochiri.errorhandling;

import com.example.member.adapter.in.web.MemberErrorMessageProvider;
import com.example.member.application.exception.MemberApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiErrorMessageCatalogTest {

    @Test
    @DisplayName("Context message provider가 API 코드의 사용자 메시지를 제공한다")
    void Context_message_provider가_API_코드의_사용자_메시지를_제공한다() {
        // given
        ApiErrorMessageCatalog catalog = new ApiErrorMessageCatalog(List.of(new MemberErrorMessageProvider()));
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);

        // when
        ApiErrorMessage message = catalog.messageFor(code);

        // then
        assertThat(message.title()).isEqualTo("회원 조회 실패");
        assertThat(message.detail()).isEqualTo("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("사용자 메시지가 등록되지 않은 API 코드는 조회할 수 없다")
    void 사용자_메시지가_등록되지_않은_API_코드는_조회할_수_없다() {
        // given
        ApiErrorMessageCatalog catalog = new ApiErrorMessageCatalog(List.of());
        ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);

        // when & then
        assertThatThrownBy(() -> catalog.messageFor(code))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(code.value());
    }
}
