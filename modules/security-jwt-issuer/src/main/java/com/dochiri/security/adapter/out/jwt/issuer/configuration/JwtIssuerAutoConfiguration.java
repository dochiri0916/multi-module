package com.dochiri.security.adapter.out.jwt.issuer.configuration;

import com.dochiri.security.adapter.out.jwt.issuer.JjwtRefreshTokenVerifierAdapter;
import com.dochiri.security.adapter.out.jwt.issuer.JjwtTokenIssuerAdapter;
import com.dochiri.security.adapter.out.jwt.issuer.SystemCurrentTimeAdapter;
import com.dochiri.security.adapter.out.jwt.issuer.UuidTokenIdGeneratorAdapter;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Clock;

@AutoConfiguration
@ConditionalOnClass(Jwts.class)
@ConditionalOnProperty(prefix = "jwt", name = "secret")
@EnableConfigurationProperties(JwtIssuerProperties.class)
@Component
public class JwtIssuerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TokenIssuerPort.class)
    RotatingTokenIssuerPort tokenIssuerPort(JwtIssuerProperties properties) {
        return new JjwtTokenIssuerAdapter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTokenVerifierPort.class)
    RefreshSessionTokenVerifierPort refreshTokenVerifierPort(
            JwtIssuerProperties properties,
            ObjectProvider<Clock> clockProvider
    ) {
        return new JjwtRefreshTokenVerifierAdapter(
                properties,
                clockProvider.getIfAvailable(Clock::systemUTC)
        );
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTimePort.class)
    CurrentTimePort currentTimePort(ObjectProvider<Clock> clockProvider) {
        return new SystemCurrentTimeAdapter(clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    @ConditionalOnMissingBean(TokenIdGeneratorPort.class)
    TokenIdGeneratorPort tokenIdGeneratorPort() {
        return new UuidTokenIdGeneratorAdapter();
    }
}
