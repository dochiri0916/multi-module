package com.dochiri.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {
    }

    @Test
    void 생성_직후에는_삭제되지_않은_상태이다() {
        TestEntity entity = new TestEntity();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    void markDeleted_호출하면_삭제_상태가_된다() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);

        boolean result = entity.markDeleted(now);

        assertThat(result).isTrue();
        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void 이미_삭제된_엔티티에_markDeleted를_호출하면_false를_반환한다() {
        TestEntity entity = new TestEntity();
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime second = LocalDateTime.of(2025, 1, 2, 12, 0);

        entity.markDeleted(first);
        boolean result = entity.markDeleted(second);

        assertThat(result).isFalse();
        assertThat(entity.getDeletedAt()).isEqualTo(first);
    }

    @Test
    void markDeleted에_null을_전달하면_NullPointerException이_발생한다() {
        TestEntity entity = new TestEntity();

        assertThatThrownBy(() -> entity.markDeleted(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 초기_상태에서_모든_필드는_null이다() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }
}
