package com.dochiri.security.jpa.configuration;

import com.dochiri.security.jpa.repository.RefreshTokenRepository;
import com.dochiri.security.jpa.service.RefreshTokenService;
import com.dochiri.security.jwt.JwtProvider;
import com.dochiri.security.jwt.JwtTokenGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;

@AutoConfiguration(before = {
        DataJpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@Import(SecurityJpaPackageRegistrar.class)
public class SecurityJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RefreshTokenService refreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenGenerator jwtTokenGenerator,
            JwtProvider jwtProvider,
            Clock clock
    ) {
        return new RefreshTokenService(refreshTokenRepository, jwtTokenGenerator, jwtProvider, clock);
    }

}