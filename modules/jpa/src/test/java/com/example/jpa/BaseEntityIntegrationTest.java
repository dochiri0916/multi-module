package com.example.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BaseEntityIntegrationTest.JpaApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:baseentity;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "dochiri.jpa.audit.system-subject=system-7"
})
class BaseEntityIntegrationTest {

    @Autowired
    private AuditableEntityRepository auditableEntityRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Test
    @DisplayName("security 모듈 없이 fallback 감사자와 QueryDSL을 함께 구성한다")
    void configuresFallbackAuditorAndQueryFactoryWithoutSecurityModule() {
        // given
        AuditableEntity entity = auditableEntityRepository.saveAndFlush(new AuditableEntity("auditor"));

        // when
        String createdBy = entity.getCreatedBy();

        // then
        assertThat(jpaQueryFactory).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(createdBy).isEqualTo("system-7");
    }

    @Test
    @DisplayName("BaseEntity를 수정하면 수정 시각과 fallback 감사자를 기록한다")
    void recordsUpdatedAuditFieldsWhenEntityChanges() {
        // given
        AuditableEntity created = auditableEntityRepository.saveAndFlush(new AuditableEntity("before"));
        AuditableEntity entity = auditableEntityRepository.findById(created.getId()).orElseThrow();
        entity.rename("after");

        // when
        AuditableEntity updated = auditableEntityRepository.saveAndFlush(entity);

        // then
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo("system-7");
    }

    @SpringBootApplication
    static class JpaApplication {
    }
}
