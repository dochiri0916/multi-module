package com.dochiri.security.adapter.in.web.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    @DisplayName("허용 origin 목록을 방어적으로 복사한다")
    void copiesAllowedOriginsDefensively() {
        // given
        List<String> mutableOrigins = new ArrayList<>(List.of("https://client.example"));
        CorsProperties properties = new CorsProperties(mutableOrigins);

        // when
        mutableOrigins.add("https://attacker.example");

        // then
        assertThat(properties.allowedOrigins()).containsExactly("https://client.example");
    }
}
