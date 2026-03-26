package com.dochiri.security.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.List;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        List<String> publicEndpoints,
        Long systemUserId
) {
    private static final List<String> DEFAULT_PUBLIC_ENDPOINTS = List.of(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    );

    public SecurityProperties {
        if (publicEndpoints == null) {
            publicEndpoints = DEFAULT_PUBLIC_ENDPOINTS;
        } else {
            LinkedHashSet<String> merged = new LinkedHashSet<>(DEFAULT_PUBLIC_ENDPOINTS);
            merged.addAll(publicEndpoints);
            publicEndpoints = List.copyOf(merged);
        }
        if (systemUserId == null) {
            systemUserId = 0L;
        }
    }
}
