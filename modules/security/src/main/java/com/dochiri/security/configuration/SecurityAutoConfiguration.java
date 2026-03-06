package com.dochiri.security.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        JwtAutoConfiguration.class,
        SecurityFilterChainAutoConfiguration.class,
        CorsAutoConfiguration.class,
        SecurityAuditAutoConfiguration.class
})
public class SecurityAutoConfiguration {
}
