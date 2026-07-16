package com.dochiri.security.adapter.out.jwt.configuration;

import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtVerifierAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtVerifierAutoConfiguration.class))
            .withPropertyValues("jwt.secret=test-secret-key-that-is-at-least-32-characters-long");

    @Test
    @DisplayName("Gateway용 JWT 구성은 Access Token 검증 Port만 제공한다")
    void providesOnlyAccessTokenVerifierForGateway() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(AccessTokenVerifierPort.class);
            assertThat(context).doesNotHaveBean(TokenIssuerPort.class);
            assertThat(context).doesNotHaveBean(RefreshTokenVerifierPort.class);
            assertThat(context).doesNotHaveBean(CurrentTimePort.class);
            assertThat(context).doesNotHaveBean(TokenIdGeneratorPort.class);
        });
    }

    @Test
    @DisplayName("소비자가 제공한 Access Token 검증 Port가 있으면 기본 Bean이 물러난다")
    void backsOffWhenConsumerProvidesAccessTokenVerifier() {
        // given
        AccessTokenVerifierPort verifier = mock(AccessTokenVerifierPort.class);
        ApplicationContextRunner runner = contextRunner
                .withBean(AccessTokenVerifierPort.class, () -> verifier);

        // when & then
        runner.run(context ->
                assertThat(context.getBean(AccessTokenVerifierPort.class)).isSameAs(verifier));
    }

    @Test
    @DisplayName("JWT secret이 없으면 Gateway 검증 기능만 비활성화한다")
    void disablesGatewayVerificationWhenJwtSecretIsMissing() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JwtVerifierAutoConfiguration.class));

        // when & then
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AccessTokenVerifierPort.class);
        });
    }
}
