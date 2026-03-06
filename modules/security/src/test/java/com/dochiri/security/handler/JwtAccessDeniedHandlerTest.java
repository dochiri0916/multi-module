package com.dochiri.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    void 접근_거부_시_403_ProblemDetail_응답을_반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("title", "FORBIDDEN");
        assertThat(body).containsEntry("detail", "접근 권한이 없습니다.");
        assertThat(body.get("instance").toString()).isEqualTo("/api/admin");
    }
}
