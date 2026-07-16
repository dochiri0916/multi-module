package com.dochiri.jpa.adapter.in.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingModuleBoundaryTest {

    @Test
    @DisplayName("JPA auditing 단독 classpath에는 QueryDSL이 포함되지 않는다")
    void doesNotIncludeQueryDslOnStandaloneClasspath() {
        // given
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // when
        boolean queryDslPresent = ClassUtils.isPresent("com.querydsl.jpa.impl.JPAQueryFactory", classLoader);

        // then
        assertThat(queryDslPresent).isFalse();
    }
}
