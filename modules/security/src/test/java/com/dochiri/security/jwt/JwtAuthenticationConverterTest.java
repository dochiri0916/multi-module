package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationConverterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    private JwtProvider jwtProvider;
    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 3600000L, 604800000L));
        converter = new JwtAuthenticationConverter(jwtProvider);
    }

    @Test
    void 액세스_토큰을_변환하면_JwtPrincipal이_포함된_Authentication이_생성된다() {
        String token = jwtProvider.generateAccessToken(1L, "USER");

        UsernamePasswordAuthenticationToken authentication = converter.convert(token);

        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo("USER");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void 리프레시_토큰으로_변환을_시도하면_BadCredentialsException이_발생한다() {
        String refreshToken = jwtProvider.generateRefreshToken(1L, "USER");

        assertThatThrownBy(() -> converter.convert(refreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("인증에 사용할 수 없는 토큰");
    }

    @Test
    void ADMIN_role이_포함된_토큰을_변환하면_ROLE_ADMIN_권한이_부여된다() {
        String token = jwtProvider.generateAccessToken(99L, "ADMIN");

        UsernamePasswordAuthenticationToken authentication = converter.convert(token);

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void ROLE_접두사가_있는_role도_ROLE_ADMIN_권한으로_정규화된다() {
        String token = jwtProvider.generateAccessToken(99L, "ROLE_ADMIN");

        UsernamePasswordAuthenticationToken authentication = converter.convert(token);
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }
}
