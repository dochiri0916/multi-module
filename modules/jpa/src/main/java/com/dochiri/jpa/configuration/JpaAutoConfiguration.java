package com.dochiri.jpa.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@AutoConfiguration(afterName = "com.dochiri.security.configuration.SecurityAutoConfiguration")
@EnableJpaAuditing
class JpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<Long> fallbackAuditorAware(Environment environment) {
        JpaAuditProperties properties = JpaAuditProperties.from(environment);
        return () -> Optional.of(properties.systemUserId());
    }

}