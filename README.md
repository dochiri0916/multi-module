# Dochiri Multi-Module Library

Java 21과 Spring Boot 4 기반 API 서비스에서 재사용할 보안, 오류 응답, JPA, 시간 설정을 제공하는 멀티 모듈 라이브러리입니다. refresh token 영역은 Hexagonal Architecture와 DDD 경계로 나뉘며, 소비 프로젝트는 역할에 맞는 공개 starter 하나만 선택합니다.

## 요구 사항

- Java 21 이상이 필요합니다.
- Gradle Wrapper 9.3.1을 사용합니다.
- Spring Boot 4.0.3 dependency BOM을 사용합니다.
- `security-jpa` 사용 시 MySQL 8.4 이상이 필요합니다. Connector/J은 starter가 제공합니다.

## 공개 Starter

| artifactId | Gradle project | 책임 |
| --- | --- | --- |
| `dochiri-service-starter` | `modules:service-starter` | 모놀리식 및 일반 MSA 서비스용 Web/JPA/MySQL/Flyway 조합 |
| `dochiri-msa-gateway-starter` | `modules:msa-gateway-starter` | DB 없는 MSA Gateway용 Access Token 검증 조합 |
| `dochiri-msa-auth-starter` | `modules:msa-auth-starter` | MSA 인증 서버용 JWT 발급·refresh session·MySQL 조합 |

소비 프로젝트에 지원하는 직접 의존점은 위 세 개뿐입니다. `error-handling-starter`, `jpa-*`, `time`, `security-*`는 starter를 구성하는 내부 모듈이며 직접 사용 계약으로 문서화하지 않습니다.

## 내부 모듈

| Gradle project | 책임 |
| --- | --- |
| `modules:error-handling-starter` | Spring 공식 ProblemDetail fallback과 Web MVC·Bean Validation |
| `modules:jpa`, `modules:jpa-auditing`, `modules:jpa-querydsl` | JPA auditing과 QueryDSL 자동 구성 |
| `modules:time` | 교체 가능한 `Clock`과 timezone 설정 |
| `modules:security-domain`, `modules:security-application` | 보안 Domain과 UseCase/Port |
| `modules:security-error-webmvc`, `modules:security-webmvc` | 보안 예외와 필터 401/403 Web Adapter |
| `modules:security-jwt`, `modules:security-jwt-issuer` | Access Token 검증과 인증 서버 token 발급 Adapter |
| `modules:security-jpa` | refresh session MySQL Adapter와 migration |

## 의존 방향

```text
security-domain
      ^
security-application
      ^
      +-- security-jwt          (Access Token 검증)
      +-- security-jwt-issuer   (발급·Refresh Token 검증)
      +-- security-jpa ------> jpa-auditing
      +-- security-error-webmvc   (보안 Context Advice)
      +-- security-webmvc --------> security-error-webmvc

jpa          -> jpa-auditing + jpa-querydsl
error-handling-starter -> Web MVC + Bean Validation
service-starter -> error-handling-starter + jpa + time + MySQL/Flyway
msa-gateway-starter -> security-jwt + security-webmvc + error-handling-starter + time
msa-auth-starter -> service-starter + security-error-webmvc + security-jwt-issuer + security-jpa
```

- Domain은 Spring, JPA, Lombok, Adapter를 알지 못합니다.
- Application은 같은 Domain과 자신이 소유한 Port만 사용하며, 실용적 예외로 `@Service`와 메서드 단위 `@Transactional`만 사용합니다.
- JJWT `Claims`, Spring Data repository, JPA Entity는 Adapter 밖으로 노출하지 않습니다.
- 인증 주체는 DB 기술 키가 아닌 `AuthenticationSubject(String)`으로 표현합니다.

## MSA 빠른 시작

일반 서비스는 JWT를 알지 않고 자기 DB만 설정합니다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-service-starter:1.0.0'
}
```

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://order-db:3306/orders?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
export SPRING_DATASOURCE_USERNAME='order_app'
export SPRING_DATASOURCE_PASSWORD='secret'
```

이것으로 WebMVC, 오류 처리, JPA auditing/QueryDSL, Connector/J, Flyway, `Clock`이 연결됩니다. JWT 모듈과 `refresh_sessions` migration은 classpath에 들어오지 않습니다.

API Gateway는 DB 없이 Access Token만 검증합니다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-msa-gateway-starter:1.0.0'
}
```

```bash
export JWT_SECRET='32자 이상의 운영 비밀키'
```

Gateway에는 `AccessTokenVerifierPort`, JWT filter와 `SecurityFilterChain`만 구성됩니다. 토큰 발급·refresh 검증 Port, token ID generator, DataSource는 구성되지 않습니다.

인증 서비스만 JWT 발급과 refresh session DB를 사용합니다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-msa-auth-starter:1.0.0'
}
```

```bash
export JWT_SECRET='Gateway와 공유하는 32자 이상의 운영 비밀키'
export SPRING_DATASOURCE_URL='jdbc:mysql://auth-db:3306/auth?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
export SPRING_DATASOURCE_USERNAME='auth_app'
export SPRING_DATASOURCE_PASSWORD='secret'
```

인증 서비스에는 발급·refresh 검증·회전·폐기·cleanup UseCase, 보안 Application 예외의 RFC 9457 매핑과 `refresh_sessions` migration이 연결되지만 Access Token 검증 filter는 들어오지 않습니다. 현재 HMAC 방식에서는 Gateway와 인증 서비스만 같은 secret을 사용하며 일반 서비스에는 secret을 배포하지 않습니다.

각 서비스는 같은 datasource property 이름에 서로 다른 URL과 DB 계정을 제공합니다. Spring Boot가 환경 변수를 직접 바인딩하므로 세 역할 모두 `application.yml`, 별도 `@Import`, scan 설정이나 Bean 조립이 필수가 아닙니다.

Gateway starter는 검증 결과를 `JwtPrincipal`로 Security Context에 저장합니다. 실제 downstream 전달은 사용하는 Gateway route 기술이 소유합니다. route filter는 외부 요청의 subject/role 헤더를 먼저 제거하고 검증된 값만 다시 넣어야 하며, 일반 서비스는 외부에서 직접 접근할 수 없도록 네트워크 정책이나 mTLS로 보호합니다. 주문 소유권 같은 도메인 권한 판단은 일반 서비스에 남깁니다.

일반 서비스는 `service-starter` 하나로 JPA auditing과 QueryDSL 구성을 함께 받습니다. 내부 JPA 모듈을 직접 조합하는 방식은 지원하는 소비 계약이 아닙니다.

## 선택 설정

인증 서버의 access token TTL 1시간과 refresh token TTL 7일, 모든 서비스의 timezone `Asia/Seoul`, Gateway의 Swagger 비공개, 인증 DB의 system auditor `system`이 기본값입니다. 다른 정책이 필요할 때만 설정합니다.

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-ttl: 1h
  refresh-token-ttl: 7d

security:
  swagger-public: false
  audit:
    system-subject: system

cors:
  allowed-origins:
    - https://app.example.com

time:
  timezone: Asia/Seoul

dochiri:
  jpa:
    audit:
      system-subject: system
```

| property | 기본값 | 설명 |
| --- | --- | --- |
| `jwt.secret` | 없음 | 최소 32자의 HMAC secret. JWT Adapter 활성화 시 필수 |
| `jwt.access-token-ttl` | `1h` | 양수 `Duration` |
| `jwt.refresh-token-ttl` | `7d` | 양수 `Duration` |
| `security.swagger-public` | `false` | Swagger 경로 공개 여부 |
| `security.audit.system-subject` | `system` | Security Context가 없을 때 security-jpa 감사자 |
| `cors.allowed-origins` | 빈 목록 | 허용 origin. `*`이면 credentials는 자동 비활성화 |
| `time.timezone` | `Asia/Seoul` | 기본 `Clock`의 Zone ID |
| `dochiri.jpa.audit.system-subject` | `system` | 일반 JPA auditing fallback 감사자 |

소비자가 같은 타입의 Bean을 제공하면 기본 `Clock`, `AccessTokenVerifierPort`, `TokenIssuerPort`, refresh verifier, token ID generator, 보안 handler, `SecurityFilterChain`, `CorsConfigurationSource`, `AuditorAware`, `JPAQueryFactory`는 물러납니다.

## 보안 UseCase

발급, 검증, 회전, 단건 폐기, 전체 폐기는 각각 하나의 Inbound Port입니다. 기존 발급·검증·폐기 API는 유지되며 rotation은 추가 계약입니다.

```java
IssueTokensResult issued = issueTokensUseCase.execute(
        new IssueTokensCommand(
                new AuthenticationSubject(memberId),
                new AuthenticationRole("MEMBER")
        )
);

VerifyRefreshTokenResult verified = verifyRefreshTokenUseCase.execute(
        new VerifyRefreshTokenQuery(new EncodedToken(refreshToken))
);

RotateTokensResult rotated = rotateTokensUseCase.execute(
        new RotateTokensCommand(new EncodedToken(refreshToken))
);

RevokeRefreshTokenResult revoked = revokeRefreshTokenUseCase.execute(
        new RevokeRefreshTokenCommand(new EncodedToken(refreshToken))
);

RevokeAllRefreshTokensResult allRevoked = revokeAllRefreshTokensUseCase.execute(
        new RevokeAllRefreshTokensCommand(new AuthenticationSubject(memberId))
);
```

동일한 refresh token으로 동시에 rotation하면 한 요청만 성공합니다. 이미 교체된 token이 다시 제출되면 replay로 판단해 세션 전체를 폐기하므로, 클라이언트는 refresh 요청을 single-flight로 묶어야 합니다. role은 세션 발급 시점의 snapshot이며 권한 변경 시 소비 애플리케이션이 `RevokeAllRefreshTokensUseCase`를 호출합니다.

Controller는 구체 Service나 Repository가 아니라 `*UseCase`만 호출합니다. 소비 Context의 `MemberId`와 `AuthenticationSubject` 변환도 소비 Web/Application 경계에서 명시합니다.

## 공개 API와 인증 principal

공개 endpoint는 path 문자열 목록 대신 handler metadata로 선언합니다.

```java
@PublicApi
@PostMapping("/api/auth/login")
LoginResponse login(@RequestBody LoginRequest request) {
    // 소비 Application UseCase 호출
}
```

`@PublicApi`는 class 또는 method에 선언할 수 있습니다. Swagger endpoint는 `security.swagger-public=true`일 때만 공개됩니다.

인증된 요청의 principal은 문자열 subject와 role을 가집니다.

```java
@GetMapping("/api/me")
MeResponse me(@AuthenticationPrincipal JwtPrincipal principal) {
    return new MeResponse(principal.subject().value(), principal.role().value());
}
```

## 오류 응답

일반 서비스, Gateway, 인증 서버는 역할별 공개 starter를 의존하는 것만으로 오류 처리가 활성화됩니다. 내부 `error-handling-starter`를 별도로 선언하지 않습니다.

별도 `@Import`, component scan, validation 구현체는 필요하지 않습니다. Spring MVC/Bean Validation 오류와 처리되지 않은 예외의 RFC 9457 응답이 자동 구성됩니다.

Domain/Application 예외는 Spring Web을 모르는 plain unchecked exception입니다. 비즈니스 예외의 HTTP 의미는 소비 Context의 Web Adapter가 `@RestControllerAdvice`와 `@ExceptionHandler`로 직접 변환합니다. 공통 handler는 특정 비즈니스 예외를 import하지 않습니다.

```json
{
  "type": "/problems/authentication-required",
  "title": "인증 필요",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/me"
}
```

Validation 오류는 Spring의 공식 `ResponseEntityExceptionHandler` 동작을 사용합니다. custom validation 원문이나 rejected value를 별도 확장 필드로 복사하지 않습니다. Context별 예제는 [ERROR_HANDLING.md](ERROR_HANDLING.md)를 참고하시기 바랍니다.

## MySQL refresh session migration

`security-jpa`는 다음 versioned migration을 소유합니다.

```text
classpath:db/migration/dochiri-security/V20260715160000__create_refresh_sessions.sql
```

`refresh_sessions`는 다음 계약을 가집니다.

- DB 기술 키 `id`와 locking용 `version`은 외부에 노출하지 않습니다.
- 안정적인 `session_id`, 현재 유효한 `current_token_id`, subject와 role snapshot을 저장합니다.
- 문자열 식별자는 대소문자를 구분하는 `ascii_bin` 또는 `utf8mb4_0900_bin` collation을 사용합니다.
- session/token unique 제약과 전체 폐기·만료 정리에 필요한 index를 제공합니다.
- JPA 객체 연관관계 없이 Aggregate 식별 값만 저장합니다.

`security-jpa`와 `msa-auth-starter`만 Flyway 보안 migration을 제공합니다. 일반 `service-starter`는 소비 서비스가 소유한 `db/migration`만 실행합니다. 운영에서는 `spring.jpa.hibernate.ddl-auto=validate`, JDBC session timezone UTC를 권장하며 Hibernate로 schema를 생성하지 않습니다.

정리 스케줄은 소비 애플리케이션이 소유합니다. 보관 기준과 batch 크기(1~1000)를 전달하면 한 batch만 삭제하고, 더 호출할 가능성을 결과로 알려줍니다.

```java
CleanupRefreshSessionsResult cleanup = cleanupRefreshSessionsUseCase.execute(
        new CleanupRefreshSessionsCommand(
                new CurrentTime(expiredRetentionCutoff),
                new RevokedAt(revokedRetentionCutoff),
                500
        )
);
```

구체적인 datasource와 운영 정책은 [SECURITY_JPA.md](SECURITY_JPA.md)를 참고하시기 바랍니다.

## 품질 게이트

```bash
./gradlew check
./gradlew check -PchangedCoverageBaseRef=origin/main
./gradlew :modules:security-domain:pitest :modules:security-application:pitest
```

`check`는 다음을 포함합니다.

- 전체 단위·통합·소비자 조합 테스트
- JWT가 없는 `service-starter`, DB가 없는 `msa-gateway-starter`, 인증 DB를 소유하는 `msa-auth-starter` 소비자 스모크 테스트
- Checkstyle, PMD, SpotBugs
- 계층 및 모듈 의존 방향 검증
- 한국어 `@DisplayName`과 given/when/then 테스트 규칙
- Domain 95/90, Application 90/85, Adapter 80/70, 전체 85/80 line/branch coverage
- 선택 시 변경 production 코드 90/85 line/branch coverage

PIT 기준은 Domain/Application mutation score 80%, test strength 85%입니다. GitHub Actions는 변경 커버리지와 PIT까지 실행합니다.

Maven Local에 배포한 실제 공개 좌표는 `tool-test-lab/starter-probe`에서 별도 소비자 빌드로 검증합니다. 이 probe는 프로젝트 의존성이 아니라 `com.dochiri:dochiri-service-starter:1.0.0`만 해석해 Web MVC, Validation, JPA 자동 구성과 ProblemDetail 응답을 확인합니다.

## 호환성 변경

이번 구조 개편으로 다음 API는 제거되었습니다.

- Web 결합형 `BaseException`, `ErrorCode`, `CommonErrorCode`, `ProblemDetails`
- `Long userId` 기반 보안 API
- JJWT `Claims`를 반환하던 public API
- JPA Entity와 Spring Data Repository를 직접 노출하던 refresh token API
- path 목록 기반 `security.public-endpoints`
- millisecond 기반 `jwt.access-expiration`, `jwt.refresh-expiration`

새 코드는 `AuthenticationSubject`, `*UseCase`, 역할별 token Port, Context별 `@ExceptionHandler`, `Duration` 설정을 사용합니다.

## 로컬 publishing

모든 내부 모듈은 공개 starter의 전이 의존성을 해석할 수 있도록 `java-library`와 `maven-publish`를 사용합니다.

```bash
./gradlew publishToMavenLocal

cd /Users/seongbin/programming/laboratory/tool-test-lab
./gradlew -p starter-probe clean test --refresh-dependencies
```

소비 프로젝트가 직접 사용하는 공개 좌표는 `dochiri-service-starter`, `dochiri-msa-gateway-starter`, `dochiri-msa-auth-starter` 세 개입니다. 나머지 좌표는 starter POM을 구성하는 내부 artifact입니다.
