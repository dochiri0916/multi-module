package com.dochiri.common.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "common")
public record CommonProperties(
        String timezone
) {
    public CommonProperties {
        if (timezone == null || timezone.isBlank()) {
            timezone = "Asia/Seoul";
        }
    }
}
