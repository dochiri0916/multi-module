package com.example.securityconsumer;

import com.dochiri.security.adapter.in.web.authentication.JwtPrincipal;
import com.dochiri.security.adapter.in.web.authentication.PublicApi;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityWebMvcConsumerSmokeTest.SecurityWebMvcApplication.class,
        properties = "jwt.secret=test-secret-key-that-is-at-least-32-characters-long"
)
@AutoConfigureMockMvc
class SecurityWebMvcConsumerSmokeTest {

    private static final String CURRENT_MEMBER_PATH = "/api/me";
    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-string-id");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Spring Data 없이 security 집계 artifact와 Web MVC 컨텍스트를 시작한다")
    void startsSecurityAggregatorWithoutSpringData() {
        // given
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // when
        boolean springDataPresent = ClassUtils.isPresent(
                "org.springframework.data.domain.AuditorAware",
                classLoader
        );

        // then
        assertThat(springDataPresent).isFalse();
        assertThat(applicationContext.getBean(AccessTokenVerifierPort.class)).isNotNull();
        assertThat(applicationContext.getBeansOfType(TokenIssuerPort.class)).isEmpty();
        assertThat(applicationContext.getBean(SecurityFilterChain.class)).isNotNull();
    }

    @Test
    @DisplayName("PublicApi가 선언된 endpoint는 path 설정 없이 공개한다")
    void publicApiEndpointIsExposedWithoutPathConfiguration() throws Exception {
        // given
        String endpoint = "/api/public";

        // when & then
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("public"));
    }

    @Test
    @DisplayName("보호된 요청의 인증 실패를 공통 SECURITY 오류 응답으로 반환한다")
    void unauthenticatedProtectedRequestReturnsCommonSecurityError() throws Exception {
        // given
        String requestId = "webmvc-request-401";

        // when & then
        mockMvc.perform(get(CURRENT_MEMBER_PATH).header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"))
                .andExpect(jsonPath("$.title").value("인증 필요"))
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.code").value("SECURITY.AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId").value(requestId))
                .andExpect(jsonPath("$.instance").value(CURRENT_MEMBER_PATH));
    }

    @Test
    @DisplayName("유효한 JWT는 문자열 subject principal로 보호 endpoint에 접근한다")
    void validJwtAccessesProtectedEndpointWithStringSubject() throws Exception {
        // given
        String accessToken = accessToken(new AuthenticationRole("MEMBER"));

        // when & then
        mockMvc.perform(get(CURRENT_MEMBER_PATH).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(SUBJECT.value()));
    }

    @Test
    @DisplayName("권한이 부족한 인증 요청을 공통 SECURITY 403 응답으로 반환한다")
    void insufficientRoleReturnsCommonSecurityForbiddenError() throws Exception {
        // given
        String accessToken = accessToken(new AuthenticationRole("MEMBER"));

        // when & then
        mockMvc.perform(get("/api/admin")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Request-Id", "webmvc-request-403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"))
                .andExpect(jsonPath("$.code").value("SECURITY.ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").value("webmvc-request-403"));
    }

    @Test
    @DisplayName("Swagger endpoint는 기본 설정에서 공개하지 않는다")
    void swaggerEndpointIsNotPublicByDefault() throws Exception {
        // given
        String swaggerEndpoint = "/v3/api-docs";

        // when & then
        mockMvc.perform(get(swaggerEndpoint))
                .andExpect(status().isUnauthorized());
    }

    private static String accessToken(AuthenticationRole role) {
        return Jwts.builder()
                .subject(SUBJECT.value())
                .claim("role", role.value())
                .claim("category", "access")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @Import(SecurityWebMvcController.class)
    static class SecurityWebMvcApplication {
    }

    @RestController
    static class SecurityWebMvcController {

        @PublicApi
        @GetMapping("/api/public")
        Map<String, String> publicEndpoint() {
            return Map.of("status", "public");
        }

        @GetMapping(CURRENT_MEMBER_PATH)
        Map<String, String> me(@AuthenticationPrincipal JwtPrincipal principal) {
            return Map.of("subject", principal.subject().value());
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        Map<String, String> admin() {
            return Map.of("status", "admin");
        }

        @GetMapping("/v3/api-docs")
        Map<String, String> swagger() {
            return Map.of("status", "swagger");
        }
    }
}
