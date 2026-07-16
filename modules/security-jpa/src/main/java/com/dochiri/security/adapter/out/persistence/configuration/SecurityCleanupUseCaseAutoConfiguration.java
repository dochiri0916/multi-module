package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.application.port.out.RefreshSessionCleanupPort;
import com.dochiri.security.application.service.CleanupRefreshSessionsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = SecurityJpaAutoConfiguration.class)
@ConditionalOnBean(RefreshSessionCleanupPort.class)
@Import(CleanupRefreshSessionsService.class)
public class SecurityCleanupUseCaseAutoConfiguration {
}
