package com.dochiri.time.adapter.in.bootstrap;

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
