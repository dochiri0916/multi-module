package com.dochiri.security.jwt;

import java.time.Instant;

public record JwtTokenResult(
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
