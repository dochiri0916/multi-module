package com.dochiri.jpa.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnClass({AuditorAware.class, EnableJpaAuditing.class})
@EnableJpaAuditing
@EnableConfigurationProperties(JpaAuditProperties.class)
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<String> fallbackAuditorAware(JpaAuditProperties properties) {
        return () -> Optional.of(properties.systemSubject());
    }
}
