package com.dochiri.time.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class TimePropertiesTest {

    @Test
    @DisplayName("시간대가 null이면 Asia Seoul 기본값을 적용한다")
    void timezone이_null이면_기본값_Asia_Seoul이_적용된다() {
        // given
        String timezone = missingValue();

        // when
        TimeProperties properties = new TimeProperties(timezone);

        // then
        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("시간대가 blank이면 Asia Seoul 기본값을 적용한다")
    void timezone이_빈문자열이면_기본값이_적용된다() {
        // given
        String timezone = "  ";

        // when
        TimeProperties properties = new TimeProperties(timezone);

        // then
        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("명시한 시간대 값을 보존한다")
    void timezone을_명시하면_해당_값이_사용된다() {
        // given
        String timezone = "UTC";

        // when
        TimeProperties properties = new TimeProperties(timezone);

        // then
        assertThat(properties.timezone()).isEqualTo("UTC");
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
