package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.authentication.PublicApi;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.DecodedAccessToken;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.TokenExpiration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityFilterChainAutoConfigurationTest.SecurityWebApplication.class,
        properties = "security.swagger-public=false"
)
@AutoConfigureMockMvc
class SecurityFilterChainAutoConfigurationTest {

    private static final String ACCESS_TOKEN = "Bearer valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("명시한 PublicApi endpoint는 인증 없이 접근할 수 있다")
    void permitsExplicitlyPublicEndpoint() throws Exception {
        // given
        String endpoint = "/api/public";

        // when
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"public\"");
    }

    @Test
    @DisplayName("보호 endpoint는 인증 토큰이 없으면 공통 인증 오류를 반환한다")
    void returnsUnauthorizedProblemWithoutAuthentication() throws Exception {
        // given
        String endpoint = "/api/protected";

        // when
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .contains("SECURITY.AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("유효한 Bearer 토큰은 보호 endpoint에 접근할 수 있다")
    void permitsProtectedEndpointWithValidBearerToken() throws Exception {
        // given
        String endpoint = "/api/protected";

        // when
        MvcResult result = mockMvc.perform(get(endpoint).header("Authorization", ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"subject\":\"member-01\"");
    }

    @Test
    @DisplayName("권한이 부족하면 공통 접근 거부 오류를 반환한다")
    void returnsForbiddenProblemForInsufficientRole() throws Exception {
        // given
        String endpoint = "/api/admin";

        // when
        MvcResult result = mockMvc.perform(get(endpoint).header("Authorization", ACCESS_TOKEN))
                .andExpect(status().isForbidden())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getContentAsString()).contains("SECURITY.ACCESS_DENIED");
    }

    @Test
    @DisplayName("Swagger endpoint는 기본 설정에서 보호한다")
    void protectsSwaggerEndpointByDefault() throws Exception {
        // given
        String endpoint = "/v3/api-docs";

        // when
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @Import({SecurityTestBeans.class, SecurityTestController.class})
    static class SecurityWebApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class SecurityTestBeans {

        @Bean
        AccessTokenVerifierPort accessTokenVerifierPort() {
            return token -> new DecodedAccessToken(
                    new AuthenticationSubject("member-01"),
                    new AuthenticationRole("MEMBER"),
                    new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"))
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @RestController
    static class SecurityTestController {

        @PublicApi
        @GetMapping("/api/public")
        Map<String, String> publicEndpoint() {
            return Map.of("status", "public");
        }

        @GetMapping("/api/protected")
        Map<String, String> protectedEndpoint() {
            return Map.of("subject", "member-01");
        }

        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        Map<String, String> adminEndpoint() {
            return Map.of("status", "admin");
        }

        @GetMapping("/v3/api-docs")
        Map<String, String> swaggerEndpoint() {
            return Map.of("status", "swagger");
        }
    }
}
