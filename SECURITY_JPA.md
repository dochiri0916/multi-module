# Security JPA Module

`modules:security-jpa`는 refresh session 영속성 Port와 Security Context 기반 JPA auditing을 구현하는 MySQL outbound Adapter다. JWT SDK, 소비 Context의 사용자 Domain, `User` Entity를 알지 못한다.

## 의존 방향

```text
security-domain <- security-application <- security-jpa -> jpa-auditing
```

Application이 소유하는 계약은 다음과 같다.

- `RefreshSessionPort`: 한 `RefreshSession` Aggregate의 저장과 조회
- `RefreshSessionBulkRevocationPort`: subject 단위 전체 폐기
- `RefreshSessionCleanupPort`: 보관 경계를 지난 세션의 제한된 batch 삭제

`security-jpa`는 각 Port를 별도 Adapter로 구현한다. 토큰 발급과 refresh 검증은 `security-jwt-issuer`가 제공하며, 자동 설정은 필요한 Port가 모두 있을 때만 해당 UseCase를 연결한다. Gateway용 `security-jwt`는 이 모듈과 연결하지 않는다.

## 빠른 시작

인증 서버 조합은 starter 하나로 사용할 수 있다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-auth-server-starter:1.0.0'
}
```

Adapter만 선택하려면 다음 두 artifact를 사용한다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-security-jwt-issuer:1.0.0'
    implementation 'com.dochiri:dochiri-security-jpa:1.0.0'
}
```

`security-jpa`가 Connector/J, Flyway core와 MySQL 지원을 모두 runtime 의존성으로 제공한다. 인증 애플리케이션은 인증 DB 접속 정보와 JWT secret만 환경 변수로 제공한다.

```bash
export JWT_SECRET='32자 이상의 운영 비밀키'
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/app?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
export SPRING_DATASOURCE_USERNAME='app'
export SPRING_DATASOURCE_PASSWORD='secret'
```

이 네 값은 애플리케이션마다 달라지는 credential/endpoint라 라이브러리가 안전하게 만들 수 없는 값이다. TTL, auditor, Flyway location, driver class, Entity/Repository scan과 UseCase 조립은 설정하지 않는다. `spring.jpa.open-in-view=false`와 `ddl-auto=validate`는 운영 정책상 원할 때 추가할 수 있지만 기동 필수값은 아니다.

## Persistence Adapter

| 구성요소 | 책임 |
| --- | --- |
| `RefreshSessionEntity` | DB row 표현. 비즈니스 규칙 없음 |
| `RefreshSessionJpaRepository` | package-private Spring Data repository와 잠금/일괄 SQL |
| `RefreshSessionMapper` | Domain과 Entity의 구조 변환만 수행하는 final utility |
| `RefreshSessionPersistenceAdapter` | Aggregate repository Port 구현 |
| `RefreshSessionBulkRevocationAdapter` | subject 단위 폐기 Port 구현 |
| `RefreshSessionCleanupAdapter` | 제한된 cleanup Port 구현 |
| `SecurityJpaPackageRegistrar` | Entity와 repository package 자동 등록 |

```text
RefreshSession                    RefreshSessionEntity
-----------------------------     --------------------------------
RefreshSessionId sessionId        Long id/version       (비공개)
TokenId currentTokenId            String sessionId/currentTokenId
AuthenticationSubject subject     String subjectId
AuthenticationRole role           String roleName
TokenExpiration expiresAt         Instant expiresAt/revokedAt
RefreshSessionStatus status
RevokedAt revokedAt
```

JPA 객체 연관관계는 사용하지 않는다. 값 있는 생성과 수정은 persistence package 안에 제한하고, repository도 외부에 공개하지 않는다.

## MySQL migration

모듈이 소유하는 versioned migration은 다음과 같다.

```text
db/migration/dochiri-security/V20260715160000__create_refresh_sessions.sql
```

`refresh_sessions`는 MySQL 8.4 기준으로 다음 계약을 제공한다.

- `BIGINT AUTO_INCREMENT` 기술 키와 `version`
- 마이크로초 정밀도의 `DATETIME(6)` 시각
- `session_id`, `current_token_id` unique 제약
- 식별자 대소문자를 구분하는 `ascii_bin`/`utf8mb4_0900_bin` collation
- 전체 폐기용 `(subject_id, revoked_at, expires_at)` index
- cleanup용 `(expires_at, id)`, `(revoked_at, id)` index
- `ENGINE=InnoDB`

운영 schema는 Flyway가 소유하며 Hibernate schema 생성은 사용하지 않는다. 시작 시 Entity 정합성까지 확인하려면 소비자가 `spring.jpa.hibernate.ddl-auto=validate`를 선택할 수 있다. migration 파일을 복사하거나 별도 location을 추가할 필요는 없다.

통합 테스트는 `mysql:8.4.10` Testcontainers에서 migration, Entity mapping, UTC/마이크로초 정밀도, collation, unique/index, 잠금과 batch SQL을 검증한다.

## UseCase 자동 연결

| 자동 설정 | 조건 | 추가되는 Inbound Port |
| --- | --- | --- |
| `SecurityUseCaseAutoConfiguration` | issuer, refresh verifier, ID/time, repository, bulk revocation Port | 발급·검증·단건/전체 폐기 |
| `SecurityRotationUseCaseAutoConfiguration` | rotating issuer, session token verifier, ID/time, repository Port | `RotateTokensUseCase` |
| `SecurityCleanupUseCaseAutoConfiguration` | cleanup Port | `CleanupRefreshSessionsUseCase` |

기존 발급·검증·폐기 계약은 유지된다. rotation과 cleanup은 조건이 충족될 때 추가되므로 개별 Adapter를 교체하는 소비자도 필요한 기능만 선택할 수 있다.

## Rotation과 replay 정책

```java
RotateTokensResult rotated = rotateTokensUseCase.execute(
        new RotateTokensCommand(new EncodedToken(rawRefreshToken))
);
```

- refresh JWT의 `sid`는 세션 동안 유지되고 `jti`만 교체된다.
- session row를 `PESSIMISTIC_WRITE`로 잠가 같은 token의 동시 rotation을 직렬화한다.
- 먼저 처리된 한 요청만 성공한다.
- 교체된 token이 다시 제시되면 replay로 판단하고 같은 세션을 폐기한다.
- replay 예외는 반환하되 폐기 트랜잭션은 커밋한다.
- refresh 절대 만료와 발급 당시 role snapshot은 rotation으로 바뀌지 않는다.

클라이언트는 refresh 요청을 single-flight로 처리해야 한다. role이 변경되면 소비 Context가 `RevokeAllRefreshTokensUseCase`를 호출해 기존 snapshot을 무효화한다.

## Cleanup 운영 정책

라이브러리는 스케줄러 주기와 보관 기간을 정하지 않는다. 소비 애플리케이션이 cutoff와 batch 크기를 전달한다.

```java
CleanupRefreshSessionsResult result = cleanupRefreshSessionsUseCase.execute(
        new CleanupRefreshSessionsCommand(
                new CurrentTime(expiredBefore),
                new RevokedAt(revokedBefore),
                500
        )
);
```

- batch 크기는 1~1000이다.
- `expires_at < expiredBefore` 또는 `revoked_at < revokedBefore`인 row만 삭제한다.
- 경계와 같은 시각은 삭제하지 않는다.
- 호출 한 번은 최대 한 batch만 삭제한다.
- `moreMayRemain()`이 `true`면 소비 스케줄러가 다음 batch를 호출할 수 있다.

한 트랜잭션이 길어지지 않도록 `moreMayRemain()`을 보고 무제한 loop를 돌리지 말고, 실행당 최대 batch 수를 소비 정책으로 제한한다.

## Security auditing

`SecurityAuditAutoConfiguration`은 Spring Data가 있을 때 `AuditorAware<String>` 기본 Bean을 제공한다.

- principal이 `JwtPrincipal`이면 `principal.subject().value()`를 사용한다.
- 다른 principal이면 `Authentication.getName()`을 사용한다.
- 인증 정보가 없거나 익명이면 `security.audit.system-subject`를 사용한다.
- 소비자가 `AuditorAware`를 등록하면 기본 구현은 물러난다.

auditing이 `security-jpa`에 격리되어 있으므로 JPA 없는 `security` 소비자는 Spring Data 타입을 로드하지 않는다.

## 실패 계약

Domain/Application 예외는 HTTP, JPA, JJWT 타입을 포함하지 않는다. 저장 여부, token 계약, role/subject/expiration 불일치, inactive/replay는 `security-webmvc`의 provider와 한국어 catalog를 통해 RFC 9457 응답으로 변환된다. cleanup Adapter 계약 위반은 외부에 내부 값이 노출되지 않는 500 오류로 매핑된다.

## 남은 확장점

- 여러 인스턴스의 대규모 세션 조회가 병목이 될 때 `RefreshSessionPort`의 Redis Adapter 검토
- 최신 role을 rotation 시점마다 조회해야 하는 서비스는 소비 Context가 소유하는 Published Language/통합 Port 설계
- 실제 release 전 schema 하위 호환 정책과 migration rollback/runbook 확정

## 검증

```bash
./gradlew :modules:security-jpa:test
./gradlew :modules:auth-server-starter:test
./gradlew check -PchangedCoverageBaseRef=origin/main
```

통합 테스트는 발급·검증·폐기, 정상 rotation, 동시 rotation, replay 세션 폐기, MySQL migration, cleanup 경계와 batch 동작을 검증한다.
