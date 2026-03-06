package com.dochiri.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ProblemDetail_형식으로_응답을_작성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityResponseWriter.write(request, response, objectMapper,
                HttpStatus.UNAUTHORIZED, "/errors/unauthorized", "UNAUTHORIZED", "인증이 필요합니다.");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("title", "UNAUTHORIZED");
        assertThat(body).containsEntry("status", 401);
        assertThat(body).containsEntry("detail", "인증이 필요합니다.");
        assertThat(body.get("type").toString()).isEqualTo("/errors/unauthorized");
        assertThat(body.get("instance").toString()).isEqualTo("/api/test");
    }

    @Test
    void FORBIDDEN_상태로_응답을_작성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityResponseWriter.write(request, response, objectMapper,
                HttpStatus.FORBIDDEN, "/errors/access-denied", "FORBIDDEN", "접근 권한이 없습니다.");

        assertThat(response.getStatus()).isEqualTo(403);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("title", "FORBIDDEN");
        assertThat(body).containsEntry("detail", "접근 권한이 없습니다.");
    }
}
