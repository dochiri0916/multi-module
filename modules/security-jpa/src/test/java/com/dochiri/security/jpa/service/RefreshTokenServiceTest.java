package com.dochiri.security.jpa.service;

import com.dochiri.security.configuration.properties.JwtProperties;
import com.dochiri.security.jpa.entity.RefreshToken;
import com.dochiri.security.jpa.repository.RefreshTokenRepository;
import com.dochiri.security.jwt.JwtProvider;
import com.dochiri.security.jwt.JwtTokenGenerator;
import com.dochiri.security.jwt.JwtTokenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenService refreshTokenService;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 3600000L, 604800000L));
        JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator(jwtProvider);
        Clock clock = Clock.fixed(Instant.parse("2026-03-08T00:00:00Z"), ZoneOffset.UTC);

        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenGenerator, jwtProvider, clock);
    }

    @Test
    void generateToken은_리프레시_토큰을_DB에_저장한다() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JwtTokenResult tokenResult = refreshTokenService.generateToken(1L, "USER");

        assertThat(tokenResult.accessToken()).isNotBlank();
        assertThat(tokenResult.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verify는_저장된_리프레시_토큰을_검증한다() {
        String refreshToken = jwtProvider.generateRefreshToken(1L, "USER");
        var claims = jwtProvider.parseAndValidate(refreshToken);
        RefreshToken storedToken = RefreshToken.issue(
                1L,
                jwtProvider.extractTokenId(claims),
                jwtProvider.extractExpiration(claims)
        );
        when(refreshTokenRepository.findByTokenId(jwtProvider.extractTokenId(claims)))
                .thenReturn(Optional.of(storedToken));

        VerifiedRefreshToken verified = refreshTokenService.verify(refreshToken);

        assertThat(verified.userId()).isEqualTo(1L);
        assertThat(verified.tokenId()).isEqualTo(jwtProvider.extractTokenId(claims));
    }

    @Test
    void 저장되지_않은_리프레시_토큰은_검증에_실패한다() {
        String refreshToken = jwtProvider.generateRefreshToken(1L, "USER");
        String tokenId = jwtProvider.extractTokenId(jwtProvider.parseAndValidate(refreshToken));
        when(refreshTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verify(refreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("저장되지 않은");
    }

    @Test
    void revoke는_저장된_토큰을_폐기한다() {
        String refreshToken = jwtProvider.generateRefreshToken(1L, "USER");
        var claims = jwtProvider.parseAndValidate(refreshToken);
        RefreshToken storedToken = RefreshToken.issue(
                1L,
                jwtProvider.extractTokenId(claims),
                jwtProvider.extractExpiration(claims)
        );
        when(refreshTokenRepository.findByTokenId(jwtProvider.extractTokenId(claims)))
                .thenReturn(Optional.of(storedToken));

        boolean revoked = refreshTokenService.revoke(refreshToken);

        assertThat(revoked).isTrue();
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void revokeAllByUserId는_활성_토큰만_폐기한다() {
        RefreshToken first = RefreshToken.issue(1L, "token-1", Instant.parse("2026-03-15T00:00:00Z"));
        RefreshToken second = RefreshToken.issue(1L, "token-2", Instant.parse("2026-03-15T00:00:00Z"));
        second.revoke(Instant.parse("2026-03-07T00:00:00Z"));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(1L)).thenReturn(List.of(first));

        int revokedCount = refreshTokenService.revokeAllByUserId(1L);

        assertThat(revokedCount).isEqualTo(1);
        assertThat(first.isRevoked()).isTrue();
    }
}
