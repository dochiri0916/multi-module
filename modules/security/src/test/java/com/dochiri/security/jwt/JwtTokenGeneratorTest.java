package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenGeneratorTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    private JwtProvider jwtProvider;
    private JwtTokenGenerator tokenGenerator;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 3600000L, 604800000L));
        tokenGenerator = new JwtTokenGenerator(jwtProvider);
    }

    @Test
    void generateToken은_액세스_토큰과_리프레시_토큰을_모두_반환한다() {
        JwtTokenResult result = tokenGenerator.generateToken(1L, "USER");

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isNotNull();
        assertThat(result.accessToken()).isNotEqualTo(result.refreshToken());
    }

    @Test
    void generateToken의_액세스_토큰은_access_카테고리이다() {
        JwtTokenResult result = tokenGenerator.generateToken(1L, "USER");

        Claims accessClaims = jwtProvider.parseAndValidate(result.accessToken());
        assertThat(jwtProvider.isAccessToken(accessClaims)).isTrue();
    }

    @Test
    void generateToken의_리프레시_토큰은_refresh_카테고리이다() {
        JwtTokenResult result = tokenGenerator.generateToken(1L, "USER");

        Claims refreshClaims = jwtProvider.parseAndValidate(result.refreshToken());
        assertThat(jwtProvider.isRefreshToken(refreshClaims)).isTrue();
    }

    @Test
    void generateAccessToken은_액세스_토큰만_반환한다() {
        String accessToken = tokenGenerator.generateAccessToken(1L, "ADMIN");

        Claims claims = jwtProvider.parseAndValidate(accessToken);
        assertThat(jwtProvider.isAccessToken(claims)).isTrue();
        assertThat(jwtProvider.extractUserId(claims)).isEqualTo(1L);
        assertThat(jwtProvider.extractRole(claims)).isEqualTo("ADMIN");
    }
}
