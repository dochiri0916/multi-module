package com.example.securityjpa;

import com.dochiri.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
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
        classes = JpaSecurityAuditingIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:securityaudit;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "time.timezone=Asia/Seoul",
                "jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
                "jwt.access-expiration=3600000",
                "jwt.refresh-expiration=604800000",
                "security.system-user-id=99"
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
    void 인증정보가_없으면_security_systemUserId가_auditor로_사용된다() {
        AuditedSecurityEntity entity = repository.saveAndFlush(new AuditedSecurityEntity("system"));

        assertThat(entity.getCreatedBy()).isEqualTo(99L);
    }

    @Test
    void JwtPrincipal_인증이면_userId가_createdBy에_반영된다() {
        authenticate(123L);

        AuditedSecurityEntity entity = repository.saveAndFlush(new AuditedSecurityEntity("authenticated"));

        assertThat(entity.getCreatedBy()).isEqualTo(123L);
    }

    @Test
    void 수정하면_인증된_userId가_updatedBy에_반영된다() {
        authenticate(123L);
        AuditedSecurityEntity created = repository.saveAndFlush(new AuditedSecurityEntity("before"));
        Instant createdAt = created.getCreatedAt();

        authenticate(456L);
        AuditedSecurityEntity entity = repository.findById(created.getId()).orElseThrow();
        entity.rename("after");

        AuditedSecurityEntity updated = repository.saveAndFlush(entity);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(updated.getUpdatedBy()).isEqualTo(456L);
    }

    private void authenticate(Long userId) {
        JwtPrincipal principal = new JwtPrincipal(userId, "USER");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
