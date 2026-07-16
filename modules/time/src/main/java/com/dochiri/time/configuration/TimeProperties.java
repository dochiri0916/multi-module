package com.dochiri.time.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "time")
public record TimeProperties(
        String timezone
) {
    public TimeProperties {
        if (timezone == null || timezone.isBlank()) {
            timezone = "Asia/Seoul";
        }
    }
}
