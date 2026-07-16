package com.dochiri.security.adapter.in.web.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityWebProperties(boolean swaggerPublic) {
}
