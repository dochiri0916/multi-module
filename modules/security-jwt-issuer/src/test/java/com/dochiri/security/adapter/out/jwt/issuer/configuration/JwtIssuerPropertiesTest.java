package com.dochiri.security.adapter.out.jwt.issuer.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtIssuerPropertiesTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    @Test
    @DisplayName("TTL 설정을 생략하면 기본 Access와 Refresh Token 만료 시간을 적용한다")
    void TTL_설정을_생략하면_기본_Access와_Refresh_Token_만료_시간을_적용한다() {
        // given
        Duration missingAccessTokenTtl = missingValue();
        Duration missingRefreshTokenTtl = missingValue();

        // when
        JwtIssuerProperties properties = new JwtIssuerProperties(
                SECRET,
                missingAccessTokenTtl,
                missingRefreshTokenTtl
        );

        // then
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("Access Token TTL이 zero이면 설정을 거부한다")
    void Access_Token_TTL이_zero이면_설정을_거부한다() {
        // given
        Duration accessTokenTtl = Duration.ZERO;

        // when & then
        assertThatThrownBy(() -> new JwtIssuerProperties(SECRET, accessTokenTtl, Duration.ofDays(7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("액세스 토큰 TTL은 양수여야 합니다.");
    }

    @Test
    @DisplayName("Access Token TTL이 음수이면 설정을 거부한다")
    void Access_Token_TTL이_음수이면_설정을_거부한다() {
        // given
        Duration accessTokenTtl = Duration.ofSeconds(-1);

        // when & then
        assertThatThrownBy(() -> new JwtIssuerProperties(SECRET, accessTokenTtl, Duration.ofDays(7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("액세스 토큰 TTL은 양수여야 합니다.");
    }

    @Test
    @DisplayName("Refresh Token TTL이 zero이면 설정을 거부한다")
    void Refresh_Token_TTL이_zero이면_설정을_거부한다() {
        // given
        Duration refreshTokenTtl = Duration.ZERO;

        // when & then
        assertThatThrownBy(() -> new JwtIssuerProperties(SECRET, Duration.ofHours(1), refreshTokenTtl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리프레시 토큰 TTL은 양수여야 합니다.");
    }

    @Test
    @DisplayName("Refresh Token TTL이 음수이면 설정을 거부한다")
    void Refresh_Token_TTL이_음수이면_설정을_거부한다() {
        // given
        Duration refreshTokenTtl = Duration.ofSeconds(-1);

        // when & then
        assertThatThrownBy(() -> new JwtIssuerProperties(SECRET, Duration.ofHours(1), refreshTokenTtl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리프레시 토큰 TTL은 양수여야 합니다.");
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
