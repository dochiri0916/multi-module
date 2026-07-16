package com.example.securityjpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = JpaSecurityAuditingIntegrationTest.JpaSecurityAuditingApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:securityaudit;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "security.audit.system-subject=system-99"
        }
)
class JpaSecurityAuditingIntegrationTest {

    @Autowired
    private AuditedSecurityEntityRepository repository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 없으면 설정한 시스템 subject를 감사자 값으로 저장한다")
    void usesConfiguredSystemSubjectWhenAuthenticationIsMissing() {
        // given
        AuditedSecurityEntity entity = new AuditedSecurityEntity("system");

        // when
        AuditedSecurityEntity saved = repository.saveAndFlush(entity);

        // then
        assertThat(saved.getCreatedBy()).isEqualTo("system-99");
    }

    @Test
    @DisplayName("인증이면 문자열 subject를 생성 감사자 값으로 저장한다")
    void usesAuthenticatedSubjectForCreatedBy() {
        // given
        authenticate("member-123");
        AuditedSecurityEntity entity = new AuditedSecurityEntity("authenticated");

        // when
        AuditedSecurityEntity saved = repository.saveAndFlush(entity);

        // then
        assertThat(saved.getCreatedBy()).isEqualTo("member-123");
    }

    @Test
    @DisplayName("엔티티를 수정하면 현재 인증 subject를 수정 감사자 값으로 저장한다")
    void usesAuthenticatedSubjectForUpdatedBy() {
        // given
        authenticate("member-123");
        AuditedSecurityEntity created = repository.saveAndFlush(new AuditedSecurityEntity("before"));
        Instant createdAt = created.getCreatedAt();
        authenticate("member-456");
        AuditedSecurityEntity entity = repository.findById(created.getId()).orElseThrow();
        entity.rename("after");

        // when
        AuditedSecurityEntity updated = repository.saveAndFlush(entity);

        // then
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(updated.getUpdatedBy()).isEqualTo("member-456");
    }

    private void authenticate(String subject) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @SpringBootApplication
    static class JpaSecurityAuditingApplication {
    }
}
