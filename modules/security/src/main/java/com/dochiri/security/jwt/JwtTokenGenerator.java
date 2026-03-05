package com.dochiri.security.jwt;

import java.time.LocalDateTime;

public class JwtTokenGenerator {

    private final JwtProvider jwtProvider;

    public JwtTokenGenerator(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public JwtTokenResult generateToken(Long userId, String role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, role);
        LocalDateTime refreshExpiresAt = jwtProvider.refreshTokenExpiresAt();

        return new JwtTokenResult(accessToken, refreshToken, refreshExpiresAt);
    }

    public String generateAccessToken(Long userId, String role) {
        return jwtProvider.generateAccessToken(userId, role);
    }
}
