package com.dochiri.security.configuration;

import com.dochiri.security.jwt.JwtAuthenticationConverter;
import com.dochiri.security.jwt.JwtAuthenticationFilter;
import com.dochiri.security.configuration.properties.JwtProperties;
import com.dochiri.security.jwt.JwtProvider;
import com.dochiri.security.jwt.JwtTokenGenerator;
import com.dochiri.security.jwt.RefreshTokenVerifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
class JwtAutoConfiguration {

    @Bean
    JwtProvider jwtProvider(JwtProperties jwtProperties, ObjectProvider<Clock> clockProvider) {
        return new JwtProvider(jwtProperties, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(JwtProvider jwtProvider) {
        return new JwtAuthenticationConverter(jwtProvider);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtAuthenticationConverter jwtAuthenticationConverter) {
        return new JwtAuthenticationFilter(jwtAuthenticationConverter);
    }

    @Bean
    JwtTokenGenerator jwtTokenGenerator(JwtProvider jwtProvider) {
        return new JwtTokenGenerator(jwtProvider);
    }

    @Bean
    RefreshTokenVerifier refreshTokenVerifier(JwtProvider jwtProvider) {
        return new RefreshTokenVerifier(jwtProvider);
    }

}