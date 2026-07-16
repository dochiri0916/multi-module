package com.dochiri.jpa.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {
    }

    @Test
    @DisplayName("새 BaseEntity의 감사 필드는 모두 비어 있다")
    void 초기_상태에서_모든_필드는_null이다() {
        // given
        TestEntity entity;

        // when
        entity = new TestEntity();

        // then
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }
}
