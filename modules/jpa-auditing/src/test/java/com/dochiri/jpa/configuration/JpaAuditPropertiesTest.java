package com.dochiri.jpa.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration.class));

    @Test
    @DisplayName("JPA 감사자의 시스템 subject를 record 설정으로 바인딩한다")
    void JPA_감사자의_시스템_subject를_record_설정으로_바인딩한다() {
        // given
        ApplicationContextRunner runner = contextRunner
                .withPropertyValues("dochiri.jpa.audit.system-subject=batch-worker");

        // when & then
        runner.run(context -> assertThat(context.getBean(JpaAuditProperties.class).systemSubject())
                .isEqualTo("batch-worker"));
    }

    @Test
    @DisplayName("JPA 감사자 설정이 없으면 system subject를 사용한다")
    void JPA_감사자_설정이_없으면_system_subject를_사용한다() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> assertThat(context.getBean(JpaAuditProperties.class).systemSubject())
                .isEqualTo("system"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JpaAuditProperties.class)
    static class TestConfiguration {
    }
}
