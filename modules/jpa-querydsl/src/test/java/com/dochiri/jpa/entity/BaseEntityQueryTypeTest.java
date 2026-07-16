package com.dochiri.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityQueryTypeTest {

    @Test
    @DisplayName("BaseEntity의 QueryDSL 메타모델은 감사 필드를 모두 노출한다")
    void exposesAllAuditingFields() {
        // given
        QBaseEntity variableQueryType = new QBaseEntity("baseEntity");
        QBaseEntity pathQueryType = new QBaseEntity(variableQueryType);
        QBaseEntity metadataQueryType = new QBaseEntity(variableQueryType.getMetadata());

        // when
        List<String> fieldNames = List.of(
            variableQueryType.createdAt.getMetadata().getName(),
            pathQueryType.updatedAt.getMetadata().getName(),
            metadataQueryType.createdBy.getMetadata().getName(),
            metadataQueryType.updatedBy.getMetadata().getName()
        );

        // then
        assertThat(fieldNames).containsExactly("createdAt", "updatedAt", "createdBy", "updatedBy");
        assertThat(List.of(variableQueryType, pathQueryType, metadataQueryType))
            .allSatisfy(queryType -> assertThat(queryType.getType()).isEqualTo(BaseEntity.class));
    }
}
