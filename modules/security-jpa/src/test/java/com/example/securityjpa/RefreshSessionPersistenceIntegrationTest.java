package com.example.refreshtokenconsumer;

import com.dochiri.security.application.exception.RefreshTokenInactiveException;
import com.dochiri.security.application.exception.RefreshTokenReplayException;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsCommand;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsResult;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsUseCase;
import com.dochiri.security.application.port.in.IssueTokensCommand;
import com.dochiri.security.application.port.in.IssueTokensResult;
import com.dochiri.security.application.port.in.IssueTokensUseCase;
import com.dochiri.security.application.port.in.RevokeRefreshTokenCommand;
import com.dochiri.security.application.port.in.RevokeRefreshTokenUseCase;
import com.dochiri.security.application.port.in.RotateTokensCommand;
import com.dochiri.security.application.port.in.RotateTokensResult;
import com.dochiri.security.application.port.in.RotateTokensUseCase;
import com.dochiri.security.application.port.in.VerifyRefreshTokenCommand;
import com.dochiri.security.application.port.in.VerifyRefreshTokenResult;
import com.dochiri.security.application.port.in.VerifyRefreshTokenUseCase;
import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(
        classes = RefreshSessionPersistenceIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                "spring.jpa.open-in-view=false",
                "jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
                "jwt.access-token-ttl=1h",
                "jwt.refresh-token-ttl=7d",
                "security.audit.system-subject=system"
        }
)
class RefreshSessionPersistenceIntegrationTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.10")
            .withDatabaseName("security_jpa")
            .withUsername("security")
            .withPassword("security")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true");

    private static final String DIRECT_INSERT_SQL = """
            insert into refresh_sessions (
                session_id,
                subject_id,
                role_name,
                current_token_id,
                expires_at,
                created_at,
                created_by,
                updated_by
            ) values (?, ?, ?, ?, current_timestamp(6), current_timestamp(6), ?, ?)
            """;
    private static final String CLEANUP_INSERT_SQL = """
            insert into refresh_sessions (
                session_id,
                subject_id,
                role_name,
                current_token_id,
                expires_at,
                revoked_at,
                created_at,
                created_by,
                updated_by
            ) values (?, ?, ?, ?, ?, ?, current_timestamp(6), ?, ?)
            """;
    private static final String SYSTEM_AUDITOR = "migration-test";
    private static final String ROLE_NAME = "MEMBER";
    private static final AuthenticationSubject SUBJECT =
            new AuthenticationSubject("550e8400-e29b-41d4-a716-446655440000");
    private static final AuthenticationRole ROLE = new AuthenticationRole(ROLE_NAME);

    @Autowired
    private IssueTokensUseCase issueTokensUseCase;

    @Autowired
    private VerifyRefreshTokenUseCase verifyRefreshTokenUseCase;

    @Autowired
    private RevokeRefreshTokenUseCase revokeRefreshTokenUseCase;

    @Autowired
    private RotateTokensUseCase rotateTokensUseCase;

    @Autowired
    private CleanupRefreshSessionsUseCase cleanupRefreshSessionsUseCase;

    @Autowired
    private RefreshSessionRepositoryPort refreshSessionRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Test
    @DisplayName("JJWT와 JPA adapter 조합으로 문자열 subject의 리프레시 토큰을 발급하고 검증한다")
    void JJWT와_JPA_adapter_조합으로_문자열_subject의_리프레시_토큰을_발급하고_검증한다() {
        // given
        IssueTokensCommand command = new IssueTokensCommand(SUBJECT, ROLE);

        // when
        IssueTokensResult issued = issueTokensUseCase.execute(command);
        VerifyRefreshTokenResult verified = verifyRefreshTokenUseCase.execute(
                new VerifyRefreshTokenCommand(issued.refreshToken())
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
    @DisplayName("폐기된 리프레시 토큰은 같은 Application 오류 계약으로 검증을 거부한다")
    void 폐기된_리프레시_토큰은_같은_Application_오류_계약으로_검증을_거부한다() {
        // given
        IssueTokensResult issued = issueTokensUseCase.execute(
                new IssueTokensCommand(SUBJECT, ROLE)
        );

        // when
        revokeRefreshTokenUseCase.execute(new RevokeRefreshTokenCommand(issued.refreshToken()));

        // then
        assertThatThrownBy(() -> verifyRefreshTokenUseCase.execute(
                new VerifyRefreshTokenCommand(issued.refreshToken())
        )).isInstanceOf(RefreshTokenInactiveException.class);
    }

    @Test
    @DisplayName("리프레시 token을 rotation하면 새 token만 검증되고 이전 token 재사용은 세션을 폐기한다")
    void 리프레시_token을_rotation하면_새_token만_검증되고_이전_token_재사용은_세션을_폐기한다() {
        // given
        IssueTokensResult issued = issueTokensUseCase.execute(
                new IssueTokensCommand(SUBJECT, ROLE)
        );

        // when
        RotateTokensResult rotated = rotateTokensUseCase.execute(
                new RotateTokensCommand(issued.refreshToken())
        );
        VerifyRefreshTokenResult verified = verifyRefreshTokenUseCase.execute(
                new VerifyRefreshTokenCommand(rotated.refreshToken())
        );

        // then
        assertThat(verified.subject()).isEqualTo(SUBJECT);
        assertThatThrownBy(() -> rotateTokensUseCase.execute(
                new RotateTokensCommand(issued.refreshToken())
        )).isInstanceOf(RefreshTokenReplayException.class);
        assertThatThrownBy(() -> verifyRefreshTokenUseCase.execute(
                new VerifyRefreshTokenCommand(rotated.refreshToken())
        )).isInstanceOf(RefreshTokenInactiveException.class);
    }

    @Test
    @DisplayName("같은 리프레시 token의 동시 rotation은 한 건만 성공하고 재사용 탐지로 세션을 폐기한다")
    void 같은_리프레시_token의_동시_rotation은_한_건만_성공하고_재사용_탐지로_세션을_폐기한다()
            throws InterruptedException, ExecutionException {
        // given
        IssueTokensResult issued = issueTokensUseCase.execute(
                new IssueTokensCommand(SUBJECT, ROLE)
        );
        CyclicBarrier startBarrier = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // when
            List<Future<RotationAttempt>> futures = List.of(
                    executor.submit(() -> rotateAfterBarrier(startBarrier, issued)),
                    executor.submit(() -> rotateAfterBarrier(startBarrier, issued))
            );
            List<RotationAttempt> attempts = List.of(futures.get(0).get(), futures.get(1).get());

            // then
            assertThat(attempts).filteredOn(RotationAttempt::succeeded).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> attempt.failure() instanceof RefreshTokenReplayException)
                    .hasSize(1);
            RotateTokensResult successfulRotation = attempts.stream()
                    .filter(RotationAttempt::succeeded)
                    .map(RotationAttempt::result)
                    .findFirst()
                    .orElseThrow();
            assertThatThrownBy(() -> verifyRefreshTokenUseCase.execute(
                    new VerifyRefreshTokenCommand(successfulRotation.refreshToken())
            )).isInstanceOf(RefreshTokenInactiveException.class);
        }
    }

    @Test
    @DisplayName("MySQL migration이 문자열 식별자의 길이와 대소문자 구분 collation을 생성한다")
    void MySQL_migration이_문자열_식별자의_길이와_대소문자_구분_collation을_생성한다() {
        // given
        String tableName = "refresh_sessions";

        // when
        Integer subjectLength = jdbcTemplate.queryForObject(
                "select character_maximum_length from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = 'subject_id'",
                Integer.class,
                tableName
        );
        String subjectCollation = jdbcTemplate.queryForObject(
                "select collation_name from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = 'subject_id'",
                String.class,
                tableName
        );
        String tokenIdCollation = jdbcTemplate.queryForObject(
                "select collation_name from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = 'current_token_id'",
                String.class,
                tableName
        );

        // then
        assertThat(subjectLength).isEqualTo(255);
        assertThat(subjectCollation).isEqualTo("utf8mb4_0900_bin");
        assertThat(tokenIdCollation).isEqualTo("ascii_bin");
    }

    @Test
    @DisplayName("MySQL migration이 UTC 시각을 마이크로초 정밀도로 저장한다")
    void MySQL_migration이_UTC_시각을_마이크로초_정밀도로_저장한다() {
        // given
        RefreshSessionId sessionId = new RefreshSessionId("microsecond-session-id");
        TokenId tokenId = new TokenId("microsecond-token-id");
        Instant expiration = Instant.parse("2037-12-31T23:59:59.123456Z");
        RefreshSession refreshSession = RefreshSession.issue(
                sessionId,
                tokenId,
                SUBJECT,
                ROLE,
                new TokenExpiration(expiration)
        );

        // when
        refreshSessionRepositoryPort.save(refreshSession);
        RefreshSession storedSession = refreshSessionRepositoryPort
                .findByCurrentTokenId(tokenId)
                .orElseThrow();

        // then
        assertThat(storedSession.expiresAt().value()).isEqualTo(expiration);
    }

    @Test
    @DisplayName("MySQL migration이 전체 폐기와 cleanup 조회에 필요한 복합 인덱스를 생성한다")
    void MySQL_migration이_전체_폐기와_cleanup_조회에_필요한_복합_인덱스를_생성한다() {
        // given
        String indexName = "idx_refresh_sessions_subject_revoked_expires";

        // when
        String indexedColumns = jdbcTemplate.queryForObject(
                "select group_concat(column_name order by seq_in_index separator ',') "
                        + "from information_schema.statistics "
                        + "where table_schema = database() and table_name = 'refresh_sessions' and index_name = ?",
                String.class,
                indexName
        );

        // then
        assertThat(indexedColumns).isEqualTo("subject_id,revoked_at,expires_at");
    }

    @Test
    @DisplayName("versioned migration은 중복 token 식별자 저장을 unique 제약으로 거부한다")
    void versioned_migration은_중복_token_식별자_저장을_unique_제약으로_거부한다() {
        // given
        String duplicateTokenId = "duplicate-token-id";
        int insertedRows = jdbcTemplate.update(
                DIRECT_INSERT_SQL,
                "duplicate-session-id-01",
                SUBJECT.value(),
                ROLE_NAME,
                duplicateTokenId,
                SYSTEM_AUDITOR,
                SYSTEM_AUDITOR
        );
        AtomicInteger duplicateRows = new AtomicInteger();

        // when & then
        assertThatThrownBy(() -> duplicateRows.set(jdbcTemplate.update(
                DIRECT_INSERT_SQL,
                "duplicate-session-id-02",
                SUBJECT.value(),
                ROLE_NAME,
                duplicateTokenId,
                SYSTEM_AUDITOR,
                SYSTEM_AUDITOR
        ))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(insertedRows).isEqualTo(1);
    }

    @Test
    @DisplayName("versioned migration은 subject가 없는 리프레시 토큰 저장을 거부한다")
    void versioned_migration은_subject가_없는_리프레시_토큰_저장을_거부한다() {
        // given
        String missingSubject = new HashMap<String, String>().get("missing");
        AtomicInteger insertedRows = new AtomicInteger();

        // when & then
        assertThatThrownBy(() -> insertedRows.set(jdbcTemplate.update(
                DIRECT_INSERT_SQL,
                "missing-subject-session-id",
                missingSubject,
                ROLE_NAME,
                "missing-subject-token-id",
                SYSTEM_AUDITOR,
                SYSTEM_AUDITOR
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("cleanup은 MySQL에서 보관 경계보다 오래된 만료·폐기 세션만 batch로 삭제한다")
    void cleanup은_MySQL에서_보관_경계보다_오래된_만료_폐기_세션만_batch로_삭제한다() {
        // given
        Instant cutoff = Instant.parse("2000-01-01T00:00:00Z");
        insertCleanupSession("expired", Instant.parse("1999-12-31T23:59:59Z"), null);
        insertCleanupSession("revoked", Instant.parse("2099-01-01T00:00:00Z"),
                Instant.parse("1999-12-31T23:59:59Z"));
        insertCleanupSession("expiration-boundary", cutoff, null);
        insertCleanupSession("revocation-boundary", Instant.parse("2099-01-01T00:00:00Z"), cutoff);
        CleanupRefreshSessionsCommand command = new CleanupRefreshSessionsCommand(
                new CurrentTime(cutoff),
                new RevokedAt(cutoff),
                2
        );

        // when
        CleanupRefreshSessionsResult firstBatch = cleanupRefreshSessionsUseCase.execute(command);
        CleanupRefreshSessionsResult secondBatch = cleanupRefreshSessionsUseCase.execute(command);

        // then
        assertThat(firstBatch.deletedCount()).isEqualTo(2);
        assertThat(firstBatch.moreMayRemain()).isTrue();
        assertThat(secondBatch.deletedCount()).isZero();
        assertThat(secondBatch.moreMayRemain()).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from refresh_sessions where session_id like 'cleanup-test-%'",
                Integer.class
        )).isEqualTo(2);
    }

    private void insertCleanupSession(String suffix, Instant expiresAt, Instant revokedAt) {
        jdbcTemplate.update(
                CLEANUP_INSERT_SQL,
                "cleanup-test-" + suffix,
                SUBJECT.value(),
                ROLE_NAME,
                "cleanup-token-" + suffix,
                expiresAt,
                revokedAt,
                SYSTEM_AUDITOR,
                SYSTEM_AUDITOR
        );
    }

    private RotationAttempt rotateAfterBarrier(
            CyclicBarrier startBarrier,
            IssueTokensResult issued
    ) throws InterruptedException, BrokenBarrierException {
        startBarrier.await();
        try {
            return RotationAttempt.succeeded(rotateTokensUseCase.execute(
                    new RotateTokensCommand(issued.refreshToken())
            ));
        } catch (RuntimeException exception) {
            return RotationAttempt.failed(exception);
        }
    }

    private record RotationAttempt(RotateTokensResult result, RuntimeException failure) {

        static RotationAttempt succeeded(RotateTokensResult result) {
            return new RotationAttempt(result, null);
        }

        static RotationAttempt failed(RuntimeException failure) {
            return new RotationAttempt(null, failure);
        }

        boolean succeeded() {
            return result != null;
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
