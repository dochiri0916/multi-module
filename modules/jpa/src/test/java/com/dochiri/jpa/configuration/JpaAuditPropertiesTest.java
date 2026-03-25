package com.dochiri.jpa.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditPropertiesTest {

    @Test
    void security_prefix를_우선해서_바인딩한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("security.system-user-id", "7")
                .withProperty("dochiri.jpa.audit.system-user-id", "3");

        JpaAuditProperties properties = JpaAuditProperties.from(environment);

        assertThat(properties.systemUserId()).isEqualTo(7L);
    }

    @Test
    void legacy_prefix도_여전히_지원한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("dochiri.jpa.audit.system-user-id", "5");

        JpaAuditProperties properties = JpaAuditProperties.from(environment);

        assertThat(properties.systemUserId()).isEqualTo(5L);
    }

    @Test
    void 설정이_없으면_기본값_0을_사용한다() {
        JpaAuditProperties properties = JpaAuditProperties.from(new MockEnvironment());

        assertThat(properties.systemUserId()).isEqualTo(0L);
    }
}
