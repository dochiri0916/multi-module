package com.dochiri.security.adapter.in.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    @Test
    @DisplayName("접근 거부 handler는 공통 응답 Port에 SECURITY ACCESS DENIED 코드를 전달한다")
    void forwardsAccessDeniedCodeToCommonResponsePort() throws IOException {
        // given
        AtomicReference<SecurityErrorCode> recordedCode = new AtomicReference<>();
        SecurityErrorResponsePort responsePort = (code, request, response) -> recordedCode.set(code);
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(responsePort);

        // when
        handler.handle(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new AccessDeniedException("internal")
        );

        // then
        assertThat(recordedCode.get()).isEqualTo(SecurityErrorCode.ACCESS_DENIED);
    }
}
