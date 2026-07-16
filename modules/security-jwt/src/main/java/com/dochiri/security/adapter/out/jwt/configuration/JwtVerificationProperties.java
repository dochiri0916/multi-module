package com.dochiri.security.adapter.out.jwt.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtVerificationProperties(
        @NotBlank(message = "JWT secret은 비어 있을 수 없습니다.")
        @Size(min = 32, message = "JWT secret은 HMAC-SHA256을 위해 최소 32자 이상이어야 합니다.")
        String secret
) {
}
