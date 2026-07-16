package com.dochiri.security.application.port.out;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityTokenPortBoundaryTest {

    @Test
    @DisplayName("Access Token 검증 Port는 검증 기능만 공개한다")
    void Access_Token_검증_Port는_검증_기능만_공개한다() {
        // given
        Class<?> verifierPort = AccessTokenVerifierPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(verifierPort);

        // then
        assertThat(methodNames).containsExactly("verifyAccess");
    }

    @Test
    @DisplayName("토큰 발급 Port는 Access Token 검증 기능을 공개하지 않는다")
    void 토큰_발급_Port는_Access_Token_검증_기능을_공개하지_않는다() {
        // given
        Class<?> issuerPort = TokenIssuerPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(issuerPort);

        // then
        assertThat(methodNames).containsExactly("issue");
    }

    @Test
    @DisplayName("토큰 회전 Port는 발급 계약을 확장하고 회전 기능만 추가한다")
    void 토큰_회전_Port는_발급_계약을_확장하고_회전_기능만_추가한다() {
        // given
        Class<?> rotatingIssuerPort = RotatingTokenIssuerPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(rotatingIssuerPort);

        // then
        assertThat(TokenIssuerPort.class).isAssignableFrom(rotatingIssuerPort);
        assertThat(methodNames).containsExactly("rotate");
    }

    @Test
    @DisplayName("리프레시 세션 검증 Port는 리프레시 검증 계약만 확장한다")
    void 리프레시_세션_검증_Port는_리프레시_검증_계약만_확장한다() {
        // given
        Class<?> sessionVerifierPort = RefreshSessionTokenVerifierPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(sessionVerifierPort);

        // then
        assertThat(RefreshTokenVerifierPort.class).isAssignableFrom(sessionVerifierPort);
        assertThat(methodNames).containsExactly("verifyRefreshSession");
    }

    private static Set<String> declaredMethodNames(Class<?> portType) {
        return Arrays.stream(portType.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
