package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final long ACCESS_EXPIRATION = 3600000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    private JwtProvider jwtProvider;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        jwtProvider = new JwtProvider(properties);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void 액세스_토큰을_생성하고_파싱할_수_있다() {
        String token = jwtProvider.generateAccessToken(1L, "USER");

        Claims claims = jwtProvider.parseAndValidate(token);

        assertThat(jwtProvider.extractUserId(claims)).isEqualTo(1L);
        assertThat(jwtProvider.extractRole(claims)).isEqualTo("USER");
        assertThat(jwtProvider.isAccessToken(claims)).isTrue();
        assertThat(jwtProvider.isRefreshToken(claims)).isFalse();
    }

    @Test
    void 리프레시_토큰을_생성하고_파싱할_수_있다() {
        String token = jwtProvider.generateRefreshToken(2L, "ADMIN");

        Claims claims = jwtProvider.parseAndValidate(token);

        assertThat(jwtProvider.extractUserId(claims)).isEqualTo(2L);
        assertThat(jwtProvider.extractRole(claims)).isEqualTo("ADMIN");
        assertThat(jwtProvider.extractTokenId(claims)).isNotBlank();
        assertThat(jwtProvider.extractExpiration(claims)).isAfter(Instant.now());
        assertThat(jwtProvider.isRefreshToken(claims)).isTrue();
        assertThat(jwtProvider.isAccessToken(claims)).isFalse();
    }

    @Test
    void 액세스_토큰에는_jti_클레임이_없다() {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        Claims claims = jwtProvider.parseAndValidate(token);

        assertThatThrownBy(() -> jwtProvider.extractTokenId(claims))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("jti");
    }

    @Test
    void 만료된_토큰을_파싱하면_예외가_발생한다() {
        JwtProperties expiredProperties = new JwtProperties(SECRET, 0L, 0L);
        JwtProvider expiredProvider = new JwtProvider(expiredProperties);

        String token = expiredProvider.generateAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtProvider.parseAndValidate(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("만료된 JWT");
    }

    @Test
    void 다른_비밀키로_서명된_토큰을_파싱하면_예외가_발생한다() {
        JwtProperties otherProperties = new JwtProperties(
                "another-secret-key-that-is-at-least-32-characters", ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        JwtProvider otherProvider = new JwtProvider(otherProperties);

        String token = otherProvider.generateAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtProvider.parseAndValidate(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("유효하지 않은 JWT");
    }

    @Test
    void role_클레임이_없으면_BadCredentialsException이_발생한다() {
        String token = buildToken("1", null, "access", null, Instant.now().plusMillis(ACCESS_EXPIRATION));
        Claims claims = jwtProvider.parseAndValidate(token);

        assertThatThrownBy(() -> jwtProvider.extractRole(claims))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("role");
    }

    @Test
    void sub_클레임이_숫자가_아니면_BadCredentialsException이_발생한다() {
        String token = buildToken("not-a-number", "USER", "access", null, Instant.now().plusMillis(ACCESS_EXPIRATION));
        Claims claims = jwtProvider.parseAndValidate(token);

        assertThatThrownBy(() -> jwtProvider.extractUserId(claims))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void refreshTokenExpiresAt은_주입된_Clock_기준으로_계산된다() {
        Instant fixedNow = Instant.parse("2030-01-01T00:00:00Z");
        JwtProvider fixedClockProvider = new JwtProvider(
                new JwtProperties(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION),
                Clock.fixed(fixedNow, ZoneOffset.UTC)
        );

        assertThat(fixedClockProvider.refreshTokenExpiresAt())
                .isEqualTo(fixedNow.plusMillis(REFRESH_EXPIRATION));
    }

    @Test
    void 잘못된_형식의_토큰을_파싱하면_예외가_발생한다() {
        assertThatThrownBy(() -> jwtProvider.parseAndValidate("invalid.token.value"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("유효하지 않은 JWT");
    }

    private String buildToken(String subject, String role, String category, String tokenId, Instant expiration) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim("category", category)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration));

        if (role != null) {
            builder.claim("role", role);
        }

        if (tokenId != null) {
            builder.id(tokenId);
        }

        return builder.signWith(signingKey).compact();
    }
}
