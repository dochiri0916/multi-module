package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.error.SecurityErrorResponsePort;
import com.dochiri.security.adapter.in.web.error.SecurityExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityErrorAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityExceptionHandlerAutoConfiguration.class,
                    SecurityErrorAutoConfiguration.class
            ))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("보안 Context Advice와 필터 오류 응답 구현을 함께 등록한다")
    void registersSecurityExceptionAndFilterErrorHandlers() {
        // given
        WebApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(SecurityExceptionHandler.class);
            assertThat(context).hasSingleBean(SecurityErrorResponsePort.class);
            assertThat(context).hasSingleBean(AuthenticationEntryPoint.class);
            assertThat(context).hasSingleBean(AccessDeniedHandler.class);
        });
    }

    @Test
    @DisplayName("사용자 보안 오류 Port와 handler가 있으면 기본 구현이 물러난다")
    void backsOffWhenConsumerProvidesSecurityHandlers() {
        // given
        SecurityErrorResponsePort responsePort = mock(SecurityErrorResponsePort.class);
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        AccessDeniedHandler accessDeniedHandler = mock(AccessDeniedHandler.class);
        WebApplicationContextRunner runner = contextRunner
                .withBean(SecurityErrorResponsePort.class, () -> responsePort)
                .withBean(AuthenticationEntryPoint.class, () -> entryPoint)
                .withBean(AccessDeniedHandler.class, () -> accessDeniedHandler);

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(SecurityErrorResponsePort.class);
            assertThat(context.getBean(SecurityErrorResponsePort.class)).isSameAs(responsePort);
            assertThat(context).hasSingleBean(AuthenticationEntryPoint.class);
            assertThat(context.getBean(AuthenticationEntryPoint.class)).isSameAs(entryPoint);
            assertThat(context).hasSingleBean(AccessDeniedHandler.class);
            assertThat(context.getBean(AccessDeniedHandler.class)).isSameAs(accessDeniedHandler);
        });
    }
}
