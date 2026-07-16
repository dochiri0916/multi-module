package com.dochiri.security.adapter.out.jwt.issuer.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtIssuerProperties(
        @NotBlank(message = "JWT secret은 비어 있을 수 없습니다.")
        @Size(min = 32, message = "JWT secret은 HMAC-SHA256을 위해 최소 32자 이상이어야 합니다.")
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {

    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(7);

    public JwtIssuerProperties {
        accessTokenTtl = accessTokenTtl == null ? DEFAULT_ACCESS_TOKEN_TTL : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? DEFAULT_REFRESH_TOKEN_TTL : refreshTokenTtl;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("액세스 토큰 TTL은 양수여야 합니다.");
        }
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("리프레시 토큰 TTL은 양수여야 합니다.");
        }
    }
}
