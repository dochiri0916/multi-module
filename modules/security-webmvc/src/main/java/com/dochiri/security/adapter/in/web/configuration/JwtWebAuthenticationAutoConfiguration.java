package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.authentication.JwtAuthenticationConverter;
import com.dochiri.security.adapter.in.web.authentication.JwtAuthenticationFilter;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration(
        afterName = "com.dochiri.security.adapter.out.jwt.configuration.JwtVerifierAutoConfiguration"
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnBean(AccessTokenVerifierPort.class)
public class JwtWebAuthenticationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    JwtAuthenticationConverter jwtAuthenticationConverter(AccessTokenVerifierPort accessTokenVerifierPort) {
        return new JwtAuthenticationConverter(accessTokenVerifierPort);
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtAuthenticationConverter authenticationConverter) {
        return new JwtAuthenticationFilter(authenticationConverter);
    }
}
