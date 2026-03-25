package com.dochiri.jpa.configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QueryDslAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QueryDslAutoConfiguration.class));

    @Test
    void entityManager가_있으면_JPAQueryFactory를_자동_등록한다() {
        contextRunner
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(JPAQueryFactory.class);
                    assertThat(context.getBean(JPAQueryFactory.class)).isNotNull();
                });
    }

    @Test
    void 기존_JPAQueryFactory가_있으면_자동_등록하지_않는다() {
        JPAQueryFactory customQueryFactory = mock(JPAQueryFactory.class);

        contextRunner
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .withBean(JPAQueryFactory.class, () -> customQueryFactory)
                .run(context -> {
                    assertThat(context).hasSingleBean(JPAQueryFactory.class);
                    assertThat(context.getBean(JPAQueryFactory.class)).isSameAs(customQueryFactory);
                });
    }
}
