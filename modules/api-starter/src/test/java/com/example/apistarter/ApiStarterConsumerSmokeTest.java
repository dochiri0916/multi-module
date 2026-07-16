package com.example.apistarter;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.ClassUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import javax.sql.DataSource;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = ApiStarterConsumerSmokeTest.ApiStarterApplication.class)
class ApiStarterConsumerSmokeTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.10")
            .withDatabaseName("api_starter")
            .withUsername("api_starter")
            .withPassword("api_starter")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Flyway flyway;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    @DisplayName("일반 API starter는 DB 정보만으로 Web과 JPA와 MySQL 기반을 시작한다")
    void startsApiStarterWithDatabaseConfiguration() {
        // given
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // when
        boolean jwtPortPresent = ClassUtils.isPresent(
                "com.dochiri.security.application.port.out.AccessTokenVerifierPort",
                classLoader
        );
        boolean springSecurityPresent = ClassUtils.isPresent(
                "org.springframework.security.web.SecurityFilterChain",
                classLoader
        );

        // then
        assertThat(dataSource).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
        assertThat(flyway).isNotNull();
        assertThat(clock).isNotNull();
        assertThat(jwtPortPresent).isFalse();
        assertThat(springSecurityPresent).isFalse();
    }

    @Test
    @DisplayName("일반 API starter는 서비스 migration만 실행하고 인증 테이블을 만들지 않는다")
    void runsServiceMigrationWithoutAuthenticationTable() {
        // given
        String serviceTable = "api_starter_probe";
        String authenticationTable = "refresh_sessions";

        // when
        Integer serviceTableCount = tableCount(serviceTable);
        Integer authenticationTableCount = tableCount(authenticationTable);

        // then
        assertThat(serviceTableCount).isOne();
        assertThat(authenticationTableCount).isZero();
    }

    private Integer tableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?",
                Integer.class,
                tableName
        );
    }

    @SpringBootApplication
    static class ApiStarterApplication {
    }
}
