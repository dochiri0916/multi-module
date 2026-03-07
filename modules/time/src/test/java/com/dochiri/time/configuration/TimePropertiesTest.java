package com.dochiri.time.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimePropertiesTest {

    @Test
    void timezone이_null이면_기본값_Asia_Seoul이_적용된다() {
        TimeProperties properties = new TimeProperties(null);

        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void timezone이_빈문자열이면_기본값이_적용된다() {
        TimeProperties properties = new TimeProperties("  ");

        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void timezone을_명시하면_해당_값이_사용된다() {
        TimeProperties properties = new TimeProperties("UTC");

        assertThat(properties.timezone()).isEqualTo("UTC");
    }
}
