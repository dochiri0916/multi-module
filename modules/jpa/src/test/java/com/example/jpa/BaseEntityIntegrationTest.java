package com.example.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BaseEntityIntegrationTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:baseentity;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "security.system-user-id=7"
})
class BaseEntityIntegrationTest {

    @Autowired
    private AuditableEntityRepository auditableEntityRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Test
    void security모듈이_없어도_createdBy가_fallback_auditor로_채워지고_queryFactory가_등록된다() {
        AuditableEntity entity = auditableEntityRepository.saveAndFlush(new AuditableEntity("auditor"));

        assertThat(jpaQueryFactory).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void 수정하면_updatedAt과_updatedBy가_채워진다() {
        AuditableEntity created = auditableEntityRepository.saveAndFlush(new AuditableEntity("before"));

        AuditableEntity entity = auditableEntityRepository.findById(created.getId()).orElseThrow();
        entity.rename("after");

        AuditableEntity updated = auditableEntityRepository.saveAndFlush(entity);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(7L);
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
