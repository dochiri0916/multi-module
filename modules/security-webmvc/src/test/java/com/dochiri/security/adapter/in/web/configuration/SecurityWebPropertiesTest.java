package com.dochiri.security.adapter.in.web.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityWebPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration.class));

    @Test
    @DisplayName("Swagger 공개는 기본적으로 비활성화한다")
    void Swagger_공개는_기본적으로_비활성화한다() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> assertThat(context.getBean(SecurityWebProperties.class).swaggerPublic()).isFalse());
    }

    @Test
    @DisplayName("명시적 설정이 있을 때만 Swagger 공개를 활성화한다")
    void 명시적_설정이_있을_때만_Swagger_공개를_활성화한다() {
        // given
        ApplicationContextRunner runner = contextRunner.withPropertyValues("security.swagger-public=true");

        // when & then
        runner.run(context -> assertThat(context.getBean(SecurityWebProperties.class).swaggerPublic()).isTrue());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityWebProperties.class)
    static class TestConfiguration {
    }
}
