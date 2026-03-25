package com.dochiri.security.jpa.service;

import java.time.Instant;

public record VerifiedRefreshToken(
        Long userId,
        String tokenId,
        Instant expiresAt
) {
}