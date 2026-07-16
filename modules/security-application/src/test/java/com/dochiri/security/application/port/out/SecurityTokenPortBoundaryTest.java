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
    void exposesOnlyAccessTokenVerification() {
        // given
        Class<?> verifierPort = AccessTokenVerifierPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(verifierPort);

        // then
        assertThat(methodNames).containsExactly("verifyAccess");
    }

    @Test
    @DisplayName("토큰 발급 Port는 Access Token 검증 기능을 공개하지 않는다")
    void tokenIssuerDoesNotExposeAccessTokenVerification() {
        // given
        Class<?> issuerPort = TokenIssuerPort.class;

        // when
        Set<String> methodNames = declaredMethodNames(issuerPort);

        // then
        assertThat(methodNames).containsExactly("issue");
    }

    @Test
    @DisplayName("토큰 회전 Port는 발급 계약을 확장하고 회전 기능만 추가한다")
    void rotatingTokenIssuerExtendsIssueContractAndAddsRotation() {
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
    void refreshSessionVerifierExtendsRefreshVerificationContract() {
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
