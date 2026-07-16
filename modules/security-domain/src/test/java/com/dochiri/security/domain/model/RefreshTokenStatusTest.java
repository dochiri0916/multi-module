package com.dochiri.security.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenStatusTest {

    @Test
    @DisplayName("Refresh Token 상태는 활성과 폐기 상태를 제공한다")
    void exposesActiveAndRevokedStatuses() {
        // given
        RefreshTokenStatus[] expectedStatuses = {
                RefreshTokenStatus.ACTIVE,
                RefreshTokenStatus.REVOKED
        };

        // when
        RefreshTokenStatus[] statuses = RefreshTokenStatus.values();

        // then
        assertThat(statuses).containsExactly(expectedStatuses);
    }
}
