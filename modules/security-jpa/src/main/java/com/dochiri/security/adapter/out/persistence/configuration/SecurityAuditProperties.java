package com.dochiri.security.adapter.out.persistence.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.audit")
public record SecurityAuditProperties(String systemSubject) {

    private static final String DEFAULT_SYSTEM_SUBJECT = "system";

    public SecurityAuditProperties {
        if (systemSubject == null || systemSubject.isBlank()) {
            systemSubject = DEFAULT_SYSTEM_SUBJECT;
        } else {
            systemSubject = systemSubject.strip();
        }
    }
}
