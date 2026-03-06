package com.dochiri.security.configuration.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void allowedOrigins가_null이면_빈_리스트가_된다() {
        CorsProperties properties = new CorsProperties(null);

        assertThat(properties.allowedOrigins()).isEmpty();
    }

    @Test
    void 와일드카드_origin이_포함되면_hasWildcardOrigin이_true를_반환한다() {
        CorsProperties properties = new CorsProperties(List.of("*"));

        assertThat(properties.hasWildcardOrigin()).isTrue();
    }

    @Test
    void 특정_origin만_있으면_hasWildcardOrigin이_false를_반환한다() {
        CorsProperties properties = new CorsProperties(List.of("https://example.com"));

        assertThat(properties.hasWildcardOrigin()).isFalse();
    }

    @Test
    void 여러_origin을_설정할_수_있다() {
        CorsProperties properties = new CorsProperties(
                List.of("https://example.com", "https://api.example.com"));

        assertThat(properties.allowedOrigins()).hasSize(2);
    }
}
