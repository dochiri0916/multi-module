package com.example.gatewaystarter;

import com.dochiri.security.adapter.in.web.authentication.JwtPrincipal;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = GatewaySecurityStarterConsumerSmokeTest.GatewaySecurityApplication.class,
        properties = "jwt.secret=test-secret-key-that-is-at-least-32-characters-long"
)
@AutoConfigureMockMvc
class GatewaySecurityStarterConsumerSmokeTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Gateway starter는 DB와 Token 발급 기능 없이 Access Token 검증만 구성한다")
    void configuresAccessTokenVerificationWithoutDatabaseOrIssuance() {
        // given
        ApplicationContext context = applicationContext;

        // when
        Map<String, AccessTokenVerifierPort> verifiers = context.getBeansOfType(AccessTokenVerifierPort.class);

        // then
        assertThat(verifiers).hasSize(1);
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
        assertThat(context.getBeansOfType(TokenIssuerPort.class)).isEmpty();
        assertThat(context.getBeansOfType(RefreshTokenVerifierPort.class)).isEmpty();
        assertThat(context.getBeansOfType(CurrentTimePort.class)).isEmpty();
        assertThat(context.getBeansOfType(TokenIdGeneratorPort.class)).isEmpty();
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(PasswordEncoder.class)).isEmpty();
    }

    @Test
    @DisplayName("Gateway starter는 유효한 JWT의 인증 주체를 Security Context에 저장한다")
    void storesValidJwtSubjectInSecurityContext() throws Exception {
        // given
        String accessToken = Jwts.builder()
                .subject("gateway-member")
                .claim("role", "MEMBER")
                .claim("category", "access")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        MvcResult result = mockMvc.perform(get("/gateway/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString()).contains("\"subject\":\"gateway-member\"");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(GatewaySecurityController.class)
    static class GatewaySecurityApplication {
    }

    @RestController
    static class GatewaySecurityController {

        @GetMapping("/gateway/me")
        Map<String, String> me(@AuthenticationPrincipal JwtPrincipal principal) {
            return Map.of("subject", principal.subject().value());
        }
    }
}
