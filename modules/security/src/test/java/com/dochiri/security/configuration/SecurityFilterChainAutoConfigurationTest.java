package com.dochiri.security.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityFilterChainAutoConfigurationTest {

    private final SecurityFilterChainAutoConfiguration configuration = new SecurityFilterChainAutoConfiguration();

    @Test
    void ObjectMapper_기본_빈은_모듈을_등록한_Jackson_매퍼를_생성한다() throws Exception {
        ObjectMapper objectMapper = configuration.objectMapper();

        String json = objectMapper.writeValueAsString(Map.of("now", Instant.parse("2026-03-07T09:41:44.552316Z")));

        assertThat(objectMapper).isNotNull();
        assertThat(json).contains("2026-03-07T09:41:44.552316Z");
    }
}
