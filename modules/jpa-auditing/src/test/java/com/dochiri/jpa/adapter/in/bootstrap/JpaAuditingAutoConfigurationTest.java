package com.dochiri.jpa.adapter.in.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingAutoConfigurationTest {

    @Test
    @DisplayName("설정한 시스템 subject를 fallback 감사자로 노출한다")
    void exposesConfiguredSystemSubjectAsFallbackAuditor() {
        // given
        JpaAuditingAutoConfiguration configuration = new JpaAuditingAutoConfiguration();
        JpaAuditProperties properties = new JpaAuditProperties("batch-worker");

        // when
        AuditorAware<String> auditorAware = configuration.fallbackAuditorAware(properties);

        // then
        assertThat(auditorAware.getCurrentAuditor()).contains("batch-worker");
    }
}
