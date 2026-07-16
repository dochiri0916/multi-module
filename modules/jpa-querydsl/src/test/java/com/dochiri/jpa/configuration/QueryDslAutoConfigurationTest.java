package com.dochiri.jpa.configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QueryDslAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QueryDslAutoConfiguration.class));

    @Test
    @DisplayName("EntityManager가 있으면 JPAQueryFactory를 자동 등록한다")
    void registersQueryFactoryWhenEntityManagerExists() {
        // given
        try (EntityManager entityManager = mock(EntityManager.class)) {
            // when & then
            contextRunner.withBean(EntityManager.class, () -> entityManager).run(context -> {
                assertThat(context).hasSingleBean(JPAQueryFactory.class);
                assertThat(context.getBean(JPAQueryFactory.class)).isNotNull();
            });
        }
    }

    @Test
    @DisplayName("사용자 JPAQueryFactory가 있으면 기본 Bean이 물러난다")
    void backsOffWhenCustomQueryFactoryExists() {
        // given
        JPAQueryFactory customQueryFactory = mock(JPAQueryFactory.class);

        try (EntityManager entityManager = mock(EntityManager.class)) {
            // when & then
            contextRunner
                    .withBean(EntityManager.class, () -> entityManager)
                    .withBean(JPAQueryFactory.class, () -> customQueryFactory)
                    .run(context -> assertThat(context.getBean(JPAQueryFactory.class))
                            .isSameAs(customQueryFactory));
        }
    }
}
