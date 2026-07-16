package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.adapter.out.persistence.audit.SecurityAuditorAware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAuditAutoConfiguration.class))
            .withPropertyValues("security.audit.system-subject=system-99");

    @Test
    @DisplayName("AuditorAware가 없으면 보안 컨텍스트 기반 감사자를 등록한다")
    void registersSecurityContextAuditorWhenNoAuditorExists() {
        // given
        String expectedSystemAuditor = "system-99";

        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).hasSingleBean(AuditorAware.class);
            assertThat(context).hasSingleBean(SecurityAuditorAware.class);
            assertThat(context.getBean(SecurityAuditorAware.class).getCurrentAuditor())
                    .hasValue(expectedSystemAuditor);
        });
    }

    @Test
    @DisplayName("사용자 정의 AuditorAware가 있으면 기본 감사자를 등록하지 않는다")
    void backsOffWhenCustomAuditorExists() {
        // given
        String expectedAuditor = "custom-auditor";

        // when
        contextRunner
                .withUserConfiguration(CustomAuditorConfiguration.class)
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(AuditorAware.class);
                    assertThat(context).doesNotHaveBean(SecurityAuditorAware.class);
                    assertThat(context.getBean(CustomAuditorAware.class).getCurrentAuditor())
                            .hasValue(expectedAuditor);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAuditorConfiguration {

        @Bean
        CustomAuditorAware customAuditorAware() {
            return new CustomAuditorAware();
        }
    }

    static final class CustomAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            return Optional.of("custom-auditor");
        }
    }
}
