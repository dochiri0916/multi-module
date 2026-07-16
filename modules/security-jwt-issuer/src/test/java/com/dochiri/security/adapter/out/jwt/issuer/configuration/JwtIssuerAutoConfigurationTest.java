package com.dochiri.security.adapter.out.jwt.issuer.configuration;

import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtIssuerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtIssuerAutoConfiguration.class))
            .withPropertyValues("jwt.secret=test-secret-key-that-is-at-least-32-characters-long");

    @Test
    @DisplayName("인증 서버용 JWT 구성은 발급과 Refresh Token Port만 제공한다")
    void providesIssuanceAndRefreshTokenPortsForAuthServer() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(RotatingTokenIssuerPort.class);
            assertThat(context).hasSingleBean(TokenIssuerPort.class);
            assertThat(context).hasSingleBean(RefreshSessionTokenVerifierPort.class);
            assertThat(context).hasSingleBean(RefreshTokenVerifierPort.class);
            assertThat(context).hasSingleBean(CurrentTimePort.class);
            assertThat(context).hasSingleBean(TokenIdGeneratorPort.class);
            assertThat(context).doesNotHaveBean(AccessTokenVerifierPort.class);
        });
    }

    @Test
    @DisplayName("소비자가 제공한 인증 서버 Port가 있으면 기본 Bean이 물러난다")
    void backsOffWhenConsumerProvidesAuthServerPorts() {
        // given
        RotatingTokenIssuerPort issuer = org.mockito.Mockito.mock(RotatingTokenIssuerPort.class);
        RefreshSessionTokenVerifierPort verifier =
                org.mockito.Mockito.mock(RefreshSessionTokenVerifierPort.class);
        CurrentTimePort currentTime = org.mockito.Mockito.mock(CurrentTimePort.class);
        TokenIdGeneratorPort tokenIdGenerator = org.mockito.Mockito.mock(TokenIdGeneratorPort.class);
        ApplicationContextRunner runner = contextRunner
                .withBean(RotatingTokenIssuerPort.class, () -> issuer)
                .withBean(RefreshSessionTokenVerifierPort.class, () -> verifier)
                .withBean(CurrentTimePort.class, () -> currentTime)
                .withBean(TokenIdGeneratorPort.class, () -> tokenIdGenerator);

        // when & then
        runner.run(context -> {
            assertThat(context.getBean(TokenIssuerPort.class)).isSameAs(issuer);
            assertThat(context.getBean(RefreshTokenVerifierPort.class)).isSameAs(verifier);
            assertThat(context.getBean(CurrentTimePort.class)).isSameAs(currentTime);
            assertThat(context.getBean(TokenIdGeneratorPort.class)).isSameAs(tokenIdGenerator);
        });
    }

    @Test
    @DisplayName("JWT secret이 없으면 인증 서버 JWT 기능만 비활성화한다")
    void disablesAuthServerJwtFeaturesWhenSecretIsMissing() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JwtIssuerAutoConfiguration.class));

        // when & then
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TokenIssuerPort.class);
            assertThat(context).doesNotHaveBean(RefreshTokenVerifierPort.class);
        });
    }

    @Test
    @DisplayName("소비자가 제공한 Clock으로 현재 시각을 만들고 UUID Token 식별자를 생성한다")
    void usesConsumerClockAndGeneratesUuidTokenIdentifier() {
        // given
        Instant fixedInstant = Instant.parse("2030-01-01T00:00:00Z");
        Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        ApplicationContextRunner runner = contextRunner.withBean(Clock.class, () -> clock);

        // when & then
        runner.run(context -> {
            assertThat(context.getBean(CurrentTimePort.class).currentTime().value()).isEqualTo(fixedInstant);
            assertThat(context.getBean(TokenIdGeneratorPort.class).generate().value()).matches("[0-9a-f]{32}");
        });
    }
}
