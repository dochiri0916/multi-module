package com.dochiri.security.jpa.service;

import com.dochiri.security.jpa.entity.RefreshToken;
import com.dochiri.security.jpa.repository.RefreshTokenRepository;
import com.dochiri.security.jwt.JwtProvider;
import com.dochiri.security.jwt.JwtTokenGenerator;
import com.dochiri.security.jwt.JwtTokenResult;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    @Transactional
    public JwtTokenResult generateToken(Long userId, String role) {
        JwtTokenResult tokenResult = jwtTokenGenerator.generateToken(userId, role);
        Claims claims = parseRefreshClaims(tokenResult.refreshToken());

        refreshTokenRepository.save(
                RefreshToken.issue(
                        jwtProvider.extractUserId(claims),
                        jwtProvider.extractTokenId(claims),
                        jwtProvider.extractExpiration(claims)
                )
        );

        return tokenResult;
    }

    public VerifiedRefreshToken verify(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);
        String tokenId = jwtProvider.extractTokenId(claims);
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BadCredentialsException("저장되지 않은 리프레시 토큰입니다."));
        Instant now = Instant.now(clock);

        if (!storedToken.getUserId().equals(jwtProvider.extractUserId(claims))) {
            throw new BadCredentialsException("리프레시 토큰 사용자 정보가 일치하지 않습니다.");
        }

        if (!storedToken.isActive(now)) {
            throw new BadCredentialsException("사용할 수 없는 리프레시 토큰입니다.");
        }

        return new VerifiedRefreshToken(
                storedToken.getUserId(),
                storedToken.getTokenId(),
                storedToken.getExpiresAt()
        );
    }

    public Long verifyAndExtractUserId(String refreshToken) {
        return verify(refreshToken).userId();
    }

    @Transactional
    public boolean revoke(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);
        String tokenId = jwtProvider.extractTokenId(claims);

        return refreshTokenRepository.findByTokenId(tokenId)
                .map(token -> token.revoke(Instant.now(clock)))
                .orElse(false);
    }

    @Transactional
    public int revokeAllByUserId(Long userId) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        Instant now = Instant.now(clock);
        int revokedCount = 0;

        for (RefreshToken refreshToken : refreshTokens) {
            if (refreshToken.revoke(now)) {
                revokedCount++;
            }
        }

        return revokedCount;
    }

    private Claims parseRefreshClaims(String refreshToken) {
        Claims claims = jwtProvider.parseAndValidate(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.");
        }

        return claims;
    }

}