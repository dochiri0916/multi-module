package com.dochiri.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void 인증_실패_시_401_ProblemDetail_응답을_반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("title", "UNAUTHORIZED");
        assertThat(body).containsEntry("detail", "인증이 필요합니다.");
        assertThat(body.get("instance").toString()).isEqualTo("/api/me");
    }
}
