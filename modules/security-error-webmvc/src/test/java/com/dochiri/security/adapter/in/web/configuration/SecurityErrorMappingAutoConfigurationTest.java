package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.error.SecurityErrorCodeMappingProvider;
import com.dochiri.security.adapter.in.web.error.SecurityErrorMessageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorMappingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityErrorMappingAutoConfiguration.class));

    @Test
    @DisplayName("Servlet 환경에서는 보안 오류 매핑과 메시지 provider를 등록한다")
    void registersSecurityErrorProvidersInServletApplication() {
        // given
        WebApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(SecurityErrorCodeMappingProvider.class);
            assertThat(context).hasSingleBean(SecurityErrorMessageProvider.class);
        });
    }
}
