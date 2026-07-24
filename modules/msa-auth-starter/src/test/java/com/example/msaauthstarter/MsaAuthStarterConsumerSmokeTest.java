package com.example.msaauthstarter;

import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsUseCase;
import com.dochiri.security.application.port.in.IssueTokensCommand;
import com.dochiri.security.application.port.in.IssueTokensResult;
import com.dochiri.security.application.port.in.IssueTokensUseCase;
import com.dochiri.security.application.port.in.RotateTokensUseCase;
import com.dochiri.security.application.port.in.VerifyRefreshTokenQuery;
import com.dochiri.security.application.port.in.VerifyRefreshTokenResult;
import com.dochiri.security.application.port.in.VerifyRefreshTokenUseCase;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(
        classes = MsaAuthStarterConsumerSmokeTest.MsaAuthApplication.class,
        properties = "jwt.secret=test-secret-key-that-is-at-least-32-characters-long"
)
@AutoConfigureMockMvc
class MsaAuthStarterConsumerSmokeTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.10")
            .withDatabaseName("msa_auth_starter")
            .withUsername("msa_auth_starter")
            .withPassword("msa_auth_starter")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true");

    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("auth-member");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    @DisplayName("MSA Auth starter는 발급과 Refresh Session 기능만 구성하고 Access Token은 검증하지 않는다")
    void configuresIssuanceAndRefreshWithoutAccessTokenVerification() {
        // given
        ApplicationContext context = applicationContext;

        // when
        IssueTokensUseCase issueTokensUseCase = context.getBean(IssueTokensUseCase.class);

        // then
        assertThat(issueTokensUseCase).isNotNull();
        assertThat(context.getBean(VerifyRefreshTokenUseCase.class)).isNotNull();
        assertThat(context.getBean(RotateTokensUseCase.class)).isNotNull();
        assertThat(context.getBean(CleanupRefreshSessionsUseCase.class)).isNotNull();
        assertThat(context.getBeansOfType(AccessTokenVerifierPort.class)).isEmpty();
        assertThat(ClassUtils.isPresent(
                "org.springframework.security.web.SecurityFilterChain",
                Thread.currentThread().getContextClassLoader()
        )).isFalse();
    }

    @Test
    @DisplayName("MSA Auth starter는 발급한 Refresh Token과 Session을 인증 DB에 저장한다")
    void persistsIssuedRefreshTokenAndSession() {
        // given
        IssueTokensUseCase issuer = applicationContext.getBean(IssueTokensUseCase.class);
        VerifyRefreshTokenUseCase verifier = applicationContext.getBean(VerifyRefreshTokenUseCase.class);

        // when
        IssueTokensResult issued = issuer.execute(
                new IssueTokensCommand(SUBJECT, new AuthenticationRole("MEMBER"))
        );
        VerifyRefreshTokenResult verified = verifier.execute(
                new VerifyRefreshTokenQuery(issued.refreshToken())
        );

        // then
        assertThat(verified.subject()).isEqualTo(SUBJECT);
        assertThat(jdbcTemplate.queryForObject(
                "select subject_id from refresh_sessions where current_token_id = ?",
                String.class,
                verified.tokenId().value()
        )).isEqualTo(SUBJECT.value());
    }

    @Test
    @DisplayName("MSA Auth starter는 토큰 오류를 보안 Context ProblemDetail로 변환한다")
    void mapsTokenFailureToSecurityProblemDetail() throws Exception {
        // given
        String endpoint = "/test/invalid-token";

        // when
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/invalid-token"))
                .andExpect(jsonPath("$.title").value("토큰 검증 실패"))
                .andExpect(jsonPath("$.detail").value("유효하지 않은 인증 토큰입니다."))
                .andExpect(jsonPath("$.instance").value(endpoint))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .contains("/problems/invalid-token")
                .doesNotContain("\"code\"");
    }

    @SpringBootApplication
    @Import(AuthFailureController.class)
    static class MsaAuthApplication {
    }

    @RestController
    static class AuthFailureController {

        @GetMapping("/test/invalid-token")
        void invalidToken() {
            throw InvalidTokenException.malformed();
        }
    }
}
