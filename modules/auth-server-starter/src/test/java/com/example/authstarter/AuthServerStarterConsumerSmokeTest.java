package com.example.authstarter;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMessage;
import com.dochiri.errorhandling.global.error.ApiErrorMessageCatalog;
import com.dochiri.errorhandling.global.error.ApiExceptionMapper;
import com.dochiri.errorhandling.global.error.MappedApiError;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
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
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.ClassUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = AuthServerStarterConsumerSmokeTest.AuthServerApplication.class,
        properties = "jwt.secret=test-secret-key-that-is-at-least-32-characters-long"
)
class AuthServerStarterConsumerSmokeTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.10")
            .withDatabaseName("auth_starter")
            .withUsername("auth_starter")
            .withPassword("auth_starter")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true");

    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("auth-member");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    @DisplayName("인증 서버 starter는 발급과 Refresh Session 기능만 구성하고 Access Token은 검증하지 않는다")
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
    @DisplayName("인증 서버 starter는 발급한 Refresh Token과 Session을 인증 DB에 저장한다")
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
    @DisplayName("인증 서버 starter는 Refresh Token 오류를 공통 API 오류 계약으로 변환한다")
    void mapsRefreshTokenFailureToCommonApiError() {
        // given
        ApiExceptionMapper exceptionMapper = applicationContext.getBean(ApiExceptionMapper.class);
        ApiErrorMessageCatalog messageCatalog = applicationContext.getBean(ApiErrorMessageCatalog.class);

        // when
        MappedApiError mappedError = exceptionMapper.map(InvalidTokenException.malformed()).orElseThrow();
        ApiErrorMessage message = messageCatalog.messageFor(mappedError.code());

        // then
        assertThat(mappedError.code()).isEqualTo(ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_MALFORMED));
        assertThat(mappedError.mapping().status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(message.title()).isEqualTo("토큰 검증 실패");
        assertThat(message.detail()).isEqualTo("유효하지 않은 인증 토큰입니다.");
    }

    @SpringBootApplication
    static class AuthServerApplication {
    }
}
