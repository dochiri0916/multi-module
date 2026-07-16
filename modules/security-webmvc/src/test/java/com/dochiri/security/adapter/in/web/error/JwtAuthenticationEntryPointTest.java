package com.dochiri.security.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    @Test
    @DisplayName("인증 실패 handler는 공통 응답 Port에 인증 필요 코드를 전달한다")
    void forwardsAuthenticationRequiredCodeToCommonResponsePort() throws IOException {
        // given
        AtomicReference<SecurityErrorCode> recordedCode = new AtomicReference<>();
        SecurityErrorResponsePort responsePort = (code, request, response) -> recordedCode.set(code);
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(responsePort);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(request, response, new BadCredentialsException("internal"));

        // then
        assertThat(recordedCode.get()).isEqualTo(SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }
}
