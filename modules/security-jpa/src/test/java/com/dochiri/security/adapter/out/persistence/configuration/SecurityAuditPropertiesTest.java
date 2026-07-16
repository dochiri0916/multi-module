package com.dochiri.security.adapter.out.persistence.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditPropertiesTest {

    @Test
    @DisplayName("시스템 subject가 없거나 공백이면 기본 subject를 사용한다")
    void usesDefaultSubjectForMissingOrBlankValue() {
        // given
        SecurityAuditProperties missingProperties = new SecurityAuditProperties(null);
        SecurityAuditProperties blankProperties = new SecurityAuditProperties("   ");

        // when
        String missingSubject = missingProperties.systemSubject();
        String blankSubject = blankProperties.systemSubject();

        // then
        assertThat(missingSubject).isEqualTo("system");
        assertThat(blankSubject).isEqualTo("system");
    }

    @Test
    @DisplayName("설정한 시스템 subject는 앞뒤 공백을 제거한다")
    void trimsConfiguredSubject() {
        // given
        SecurityAuditProperties properties = new SecurityAuditProperties("  system-99  ");

        // when
        String subject = properties.systemSubject();

        // then
        assertThat(subject).isEqualTo("system-99");
    }
}
