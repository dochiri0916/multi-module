package com.dochiri.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long accessTokenExpirationSeconds) {

    private static final String DEFAULT_SECRET = "change-this-secret-key-to-at-least-32-bytes";
    private static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_SECONDS = 3600L;

    public JwtProperties {
        if (!StringUtils.hasText(secret)) {
            secret = DEFAULT_SECRET;
        }
        if (accessTokenExpirationSeconds <= 0) {
            accessTokenExpirationSeconds = DEFAULT_ACCESS_TOKEN_EXPIRATION_SECONDS;
        }
    }
}
