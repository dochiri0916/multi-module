package com.dochiri.jpa.configuration;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

record JpaAuditProperties(
        Long systemUserId
) {
    static JpaAuditProperties from(Environment environment) {
        Binder binder = Binder.get(environment);
        Long systemUserId = binder.bind("security.system-user-id", Long.class)
                .orElseGet(() -> binder.bind("dochiri.jpa.audit.system-user-id", Long.class).orElse(0L));

        return new JpaAuditProperties(systemUserId);
    }
}