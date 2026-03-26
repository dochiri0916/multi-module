package com.dochiri.security.jwt;

import java.time.Instant;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtTokenGenerator {

    private final JwtProvider jwtProvider;

    public JwtTokenResult generateToken(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, role);
        Instant refreshExpiresAt = jwtProvider.extractExpiration(jwtProvider.parseAndValidate(refreshToken));

        return new JwtTokenResult(accessToken, refreshToken, refreshExpiresAt);
    }

    public String generateAccessToken(Long userId, String role) {
        return jwtProvider.generateAccessToken(userId, role);
    }
}
