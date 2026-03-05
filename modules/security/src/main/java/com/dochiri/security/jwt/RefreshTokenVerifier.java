package com.dochiri.security.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;

public class RefreshTokenVerifier {

    private final JwtProvider jwtProvider;

    public RefreshTokenVerifier(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public Long verifyAndExtractUserId(String refreshToken) {
        Claims claims = jwtProvider.parseAndValidate(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.");
        }

        return jwtProvider.extractUserId(claims);
    }
}
