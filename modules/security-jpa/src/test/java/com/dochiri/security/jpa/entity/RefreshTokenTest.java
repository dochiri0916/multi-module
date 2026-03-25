package com.dochiri.security.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    @Test
    void revoke를_처음_호출하면_토큰이_폐기된다() {
        RefreshToken refreshToken = RefreshToken.issue(1L, "token-id", Instant.parse("2026-03-15T00:00:00Z"));
        Instant now = Instant.parse("2026-03-08T00:00:00Z");

        boolean revoked = refreshToken.revoke(now);

        assertThat(revoked).isTrue();
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void 이미_폐기된_토큰을_다시_폐기하면_false를_반환한다() {
        RefreshToken refreshToken = RefreshToken.issue(1L, "token-id", Instant.parse("2026-03-15T00:00:00Z"));

        refreshToken.revoke(Instant.parse("2026-03-08T00:00:00Z"));
        boolean revoked = refreshToken.revoke(Instant.parse("2026-03-08T01:00:00Z"));

        assertThat(revoked).isFalse();
    }

    @Test
    void 만료되지_않고_폐기되지_않았으면_active다() {
        RefreshToken refreshToken = RefreshToken.issue(1L, "token-id", Instant.parse("2026-03-15T00:00:00Z"));

        assertThat(refreshToken.isActive(Instant.parse("2026-03-08T00:00:00Z"))).isTrue();
    }

    @Test
    void 만료되었으면_active가_아니다() {
        RefreshToken refreshToken = RefreshToken.issue(1L, "token-id", Instant.parse("2026-03-08T00:00:00Z"));

        assertThat(refreshToken.isActive(Instant.parse("2026-03-08T00:00:00Z"))).isFalse();
    }

    @Test
    void revoke에_null을_전달하면_예외가_발생한다() {
        RefreshToken refreshToken = RefreshToken.issue(1L, "token-id", Instant.parse("2026-03-15T00:00:00Z"));

        assertThatThrownBy(() -> refreshToken.revoke(null))
                .isInstanceOf(NullPointerException.class);
    }
}
