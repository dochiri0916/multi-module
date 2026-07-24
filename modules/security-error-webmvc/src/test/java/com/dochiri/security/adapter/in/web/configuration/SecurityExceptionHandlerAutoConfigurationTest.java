package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.error.SecurityExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityExceptionHandlerAutoConfiguration.class));

    @Test
    @DisplayName("Servlet 환경에서는 보안 Context 예외 처리기를 등록한다")
    void registersSecurityExceptionHandlerInServletApplication() {
        // given
        WebApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> assertThat(context).hasSingleBean(SecurityExceptionHandler.class));
    }
}
