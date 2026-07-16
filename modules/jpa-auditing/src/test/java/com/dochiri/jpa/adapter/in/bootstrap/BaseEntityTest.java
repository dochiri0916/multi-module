package com.dochiri.jpa.adapter.in.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    @DisplayName("새 BaseEntity의 감사 필드는 모두 비어 있다")
    void startsWithAllAuditFieldsEmpty() {
        // given
        BaseEntity entity;

        // when
        entity = new BaseEntity();

        // then
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }
}
