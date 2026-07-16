package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.adapter.out.persistence.audit.SecurityAuditorAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.AuditorAware;

@AutoConfiguration(beforeName = "com.dochiri.jpa.adapter.in.bootstrap.JpaAuditingAutoConfiguration")
@ConditionalOnClass(AuditorAware.class)
@EnableConfigurationProperties(SecurityAuditProperties.class)
@Component
public class SecurityAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    SecurityAuditorAware securityAuditorAware(SecurityAuditProperties properties) {
        return new SecurityAuditorAware(properties);
    }
}
