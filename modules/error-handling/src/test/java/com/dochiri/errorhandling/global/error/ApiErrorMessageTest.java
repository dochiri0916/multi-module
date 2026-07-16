package com.dochiri.errorhandling.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiErrorMessageTest {

    @Test
    @DisplayName("사용자 오류 title이 null이면 메시지 계약을 거부한다")
    void rejectsNullTitle() {
        // given
        String title = missingValue();

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage(title, "상세"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 title이 blank이면 메시지 계약을 거부한다")
    void rejectsBlankTitle() {
        // given
        String title = "   ";

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage(title, "상세"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 detail이 null이면 메시지 계약을 거부한다")
    void rejectsNullDetail() {
        // given
        String detail = missingValue();

        // when & then
        assertThatThrownBy(() -> new ApiErrorMessage("제목", detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 오류 detail이 blank이면 메시지 계약을 거부한다")
    void rejectsBlankDetail() {
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
