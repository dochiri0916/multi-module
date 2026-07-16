package com.dochiri.jpa.adapter.in.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    static class FixtureEntity extends BaseEntity {
    }

    @Test
    @DisplayName("새 BaseEntity의 감사 필드는 모두 비어 있다")
    void startsWithAllAuditFieldsEmpty() {
        // given
        FixtureEntity entity;

        // when
        entity = new FixtureEntity();

        // then
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }
}
