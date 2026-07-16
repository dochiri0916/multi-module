package com.dochiri.jpa.adapter.in.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dochiri.jpa.audit")
public record JpaAuditProperties(String systemSubject) {

    private static final String DEFAULT_SYSTEM_SUBJECT = "system";

    public JpaAuditProperties {
        if (systemSubject == null || systemSubject.isBlank()) {
            systemSubject = DEFAULT_SYSTEM_SUBJECT;
        } else {
            systemSubject = systemSubject.strip();
        }
    }
}
