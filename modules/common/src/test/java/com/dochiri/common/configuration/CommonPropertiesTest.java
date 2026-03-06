package com.dochiri.common.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonPropertiesTest {

    @Test
    void timezone이_null이면_기본값_Asia_Seoul이_적용된다() {
        CommonProperties properties = new CommonProperties(null);

        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void timezone이_빈문자열이면_기본값이_적용된다() {
        CommonProperties properties = new CommonProperties("  ");

        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void timezone을_명시하면_해당_값이_사용된다() {
        CommonProperties properties = new CommonProperties("UTC");

        assertThat(properties.timezone()).isEqualTo("UTC");
    }
}
