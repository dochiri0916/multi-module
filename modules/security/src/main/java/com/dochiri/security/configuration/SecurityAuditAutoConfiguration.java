package com.dochiri.security.configuration;

import com.dochiri.security.audit.SecurityAuditorAware;
import com.dochiri.security.configuration.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration(proxyBeanMethods = false)
class SecurityAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<Long> auditorAware(SecurityProperties securityProperties) {
        return new SecurityAuditorAware(securityProperties.systemUserId());
    }
}
