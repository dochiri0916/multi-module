package com.dochiri.errorhandling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiErrorMessageTest {

    @Test
    @DisplayName("사용자 오류 title이 null이면 메시지 계약을 거부한다")
    void 사용자_오류_title이_null이면_메시지_계약을_거부한다() {
        // given
        String title = missingValue();

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage(title, "상세"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 title이 blank이면 메시지 계약을 거부한다")
    void 사용자_오류_title이_blank이면_메시지_계약을_거부한다() {
        // given
        String title = "   ";

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage(title, "상세"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 detail이 null이면 메시지 계약을 거부한다")
    void 사용자_오류_detail이_null이면_메시지_계약을_거부한다() {
        // given
        String detail = missingValue();

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage("제목", detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 detail이 blank이면 메시지 계약을 거부한다")
    void 사용자_오류_detail이_blank이면_메시지_계약을_거부한다() {
        // given
        String detail = "   ";

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage("제목", detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail은 필수입니다.");
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
