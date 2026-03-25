package com.dochiri.security.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secret은 비어 있을 수 없습니다.")
        @Size(min = 32, message = "JWT secret은 HMAC-SHA256을 위해 최소 32자 이상이어야 합니다.")
        String secret,

        @Positive(message = "액세스 토큰 만료 시간은 양수여야 합니다.")
        long accessExpiration,

        @Positive(message = "리프레시 토큰 만료 시간은 양수여야 합니다.")
        long refreshExpiration
) {
}