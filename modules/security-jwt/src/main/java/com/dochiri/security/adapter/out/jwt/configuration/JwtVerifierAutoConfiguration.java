package com.dochiri.security.adapter.out.jwt.configuration;

import com.dochiri.security.adapter.out.jwt.JjwtAccessTokenVerifierAdapter;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
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
@EnableConfigurationProperties(JwtVerificationProperties.class)
@Component
public class JwtVerifierAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AccessTokenVerifierPort.class)
    AccessTokenVerifierPort accessTokenVerifierPort(
            JwtVerificationProperties properties,
            ObjectProvider<Clock> clockProvider
    ) {
        return new JjwtAccessTokenVerifierAdapter(
                properties,
                clockProvider.getIfAvailable(Clock::systemUTC)
        );
    }
}
