package com.dochiri.security.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        List<String> publicEndpoints,
        Long systemUserId
) {
    public SecurityProperties {
        if (publicEndpoints == null) {
            publicEndpoints = List.of();
        }
        if (systemUserId == null) {
            systemUserId = 0L;
        }
    }
}