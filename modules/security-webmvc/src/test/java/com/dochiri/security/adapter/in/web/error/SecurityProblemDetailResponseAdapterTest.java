package com.dochiri.security.adapter.in.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityProblemDetailResponseAdapterTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();
    private final SecurityProblemDetailResponseAdapter adapter =
            new SecurityProblemDetailResponseAdapter(objectMapper);

    @Test
    @DisplayName("인증 실패를 표준 ProblemDetail 응답으로 작성한다")
    void writesAuthenticationRequiredProblemDetail() throws IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        adapter.write(SecurityErrorCode.AUTHENTICATION_REQUIRED, request, response);

        // then
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(MediaType.parseMediaType(response.getContentType())
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        assertThat(body.get("type").asText()).isEqualTo("/problems/authentication-required");
        assertThat(body.get("title").asText()).isEqualTo("인증 필요");
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.get("detail").asText()).isEqualTo("인증이 필요합니다.");
        assertThat(body.get("instance").asText()).isEqualTo("/api/private");
        assertThat(body.has("code")).isFalse();
        assertThat(body.has("traceId")).isFalse();
        assertThat(body.has("properties")).isFalse();
    }

    @Test
    @DisplayName("인가 실패를 표준 ProblemDetail 응답으로 작성한다")
    void writesAccessDeniedProblemDetail() throws IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        adapter.write(SecurityErrorCode.ACCESS_DENIED, request, response);

        // then
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.get("type").asText()).isEqualTo("/problems/access-denied");
        assertThat(body.get("title").asText()).isEqualTo("접근 거부");
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.get("detail").asText()).isEqualTo("접근 권한이 없습니다.");
        assertThat(body.get("instance").asText()).isEqualTo("/api/admin");
    }
}
