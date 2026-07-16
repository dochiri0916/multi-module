package com.dochiri.security.adapter.in.web.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CorsAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CorsAutoConfiguration.class));

    @Test
    @DisplayName("명시한 origin에는 credentials를 허용하는 CORS 설정을 등록한다")
    void registersCredentialEnabledCorsForConfiguredOrigin() {
        // given
        WebApplicationContextRunner runner = contextRunner
                .withPropertyValues("cors.allowed-origins[0]=https://example.com");

        // when & then
        runner.run(context -> {
            CorsConfiguration configuration = configuration(context.getBean(CorsConfigurationSource.class));
            assertThat(configuration.getAllowedOrigins()).containsExactly("https://example.com");
            assertThat(configuration.getAllowCredentials()).isTrue();
        });
    }

    @Test
    @DisplayName("wildcard origin에는 credentials를 비활성화한다")
    void disablesCredentialsForWildcardOrigin() {
        // given
        WebApplicationContextRunner runner = contextRunner.withPropertyValues("cors.allowed-origins[0]=*");

        // when & then
        runner.run(context -> {
            CorsConfiguration configuration = configuration(context.getBean(CorsConfigurationSource.class));
            assertThat(configuration.getAllowedOrigins()).containsExactly("*");
            assertThat(configuration.getAllowCredentials()).isFalse();
        });
    }

    @Test
    @DisplayName("소비자가 CORS source를 제공하면 기본 Bean이 물러난다")
    void backsOffWhenConsumerProvidesCorsSource() {
        // given
        CorsConfigurationSource customSource = mock(CorsConfigurationSource.class);
        WebApplicationContextRunner runner = contextRunner
                .withBean(CorsConfigurationSource.class, () -> customSource);

        // when & then
        runner.run(context -> assertThat(context.getBean(CorsConfigurationSource.class)).isSameAs(customSource));
    }

    @Test
    @DisplayName("origin 설정이 없으면 불변 빈 목록을 사용한다")
    void usesImmutableEmptyOriginListWhenConfigurationIsMissing() {
        // given
        List<String> origins = new HashMap<String, List<String>>().get("missing");

        // when
        CorsProperties properties = new CorsProperties(origins);

        // then
        assertThat(properties.allowedOrigins()).isEmpty();
    }

    private static CorsConfiguration configuration(CorsConfigurationSource source) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        return source.getCorsConfiguration(request);
    }
}
