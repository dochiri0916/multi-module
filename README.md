# multi-module

Spring Boot 4 기반 공통 라이브러리 레포입니다. 이 레포는 실행 애플리케이션이 아니라, 다른 Spring Boot 프로젝트에서 재사용할 모듈을 관리합니다.

기본 사용 경로는 `dochiri-api-starter` 하나를 의존받는 방식입니다.

## 제공 모듈

| 구분 | artifactId | 역할 |
| --- | --- | --- |
| 기본 진입점 | `dochiri-api-starter` | `webmvc + validation + error-handling + time + jpa + security + security-jpa` 묶음 |
| 내부 모듈 | `dochiri-error-handling` | `BaseException`, `ErrorCode`, `GlobalExceptionHandler` |
| 내부 모듈 | `dochiri-time` | `Clock`, `time.timezone` 설정 |
| 내부 모듈 | `dochiri-jpa` | `BaseEntity`, JPA Auditing, `JPAQueryFactory` |
| 내부 모듈 | `dochiri-security` | JWT 발급/검증, 기본 `SecurityFilterChain`, CORS, Auditing 연동 |
| 내부 모듈 | `dochiri-security-jpa` | refresh token persistence (`RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`), `User` 엔티티는 제공하지 않음 |

소비 프로젝트는 특별한 이유가 없으면 내부 모듈을 직접 조합하지 말고 `dochiri-api-starter`를 사용하시는 편이 좋습니다.

## 기본 사용 흐름

1. 이 레포에서 테스트합니다.

```bash
./gradlew clean test
```

2. 이 레포를 로컬 Maven 저장소에 배포합니다.

```bash
./gradlew publishToMavenLocal
```

3. 별도 Spring Boot 프로젝트에서 `mavenLocal()`로 `dochiri-api-starter`를 의존받습니다.
4. 소비 프로젝트를 `bootRun`으로 기동해서 실제 계약을 확인합니다.

이 레포 자체는 `bootRun` 대상이 아닙니다.

## 소비 프로젝트 시작 방법

가장 단순한 개인 로컬 소비 프로젝트 기준입니다.

`start.spring.io`에서는 최소로 아래 정도만 받아도 됩니다.

- `H2 Database`

그 다음 이 레포에서 먼저 `./gradlew publishToMavenLocal`을 실행한 뒤, 소비 프로젝트 `build.gradle`에 로컬 Maven과 starter를 추가합니다.

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.dochiri:dochiri-api-starter:0.0.1-SNAPSHOT'

    runtimeOnly 'com.h2database:h2'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

중요한 점:

- `mavenLocal()`이 없으면 로컬 publish artifact를 찾지 못합니다.
- 이 레포에서 코드나 버전을 바꾸셨다면 다시 `./gradlew publishToMavenLocal`을 실행하셔야 합니다.
- `dochiri-api-starter`는 `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `dochiri-error-handling`, `dochiri-time`, `dochiri-security-jpa`를 함께 제공합니다.
- DB 드라이버는 소비 프로젝트에서 직접 선택해서 추가하셔야 합니다. 예시에서는 H2를 사용합니다.

## starter 적용 의존성

`dochiri-api-starter`를 의존하면 아래 항목을 함께 받습니다.

- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-validation`
- `com.dochiri:dochiri-error-handling`
- `com.dochiri:dochiri-time`
- `com.dochiri:dochiri-security-jpa`

즉 소비 프로젝트에서 별도로 validation starter를 추가하지 않아도 `@Valid`, `@Validated`, `jakarta.validation.constraints.*`를 바로 사용하실 수 있습니다.
`dochiri-security-jpa`가 `dochiri-jpa`, `dochiri-security`까지 함께 끌고 오므로 refresh token 저장 기능도 바로 사용하실 수 있습니다.
다만 사용자 도메인 자체는 포함하지 않습니다. `User`, `Member`, `Account` 같은 엔티티와 repository는 소비 프로젝트가 직접 가져야 합니다.

## 필수 설정

소비 프로젝트의 `application.yml` 예시:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    open-in-view: false

time:
  timezone: Asia/Seoul
jwt:
  secret: test-secret-key-that-is-at-least-32-characters-long
  access-expiration: 3600000
  refresh-expiration: 604800000

cors:
  allowed-origins:
    - http://localhost:3000

security:
  public-endpoints:
    - /api/public/**
    - /api/auth/**
  system-user-id: 0
```

주의:

- `time`, `jwt`, `cors`, `security`는 `spring:` 아래가 아니라 최상위 prefix입니다.
- `jwt.secret`은 32자 이상이어야 합니다.
- JPA auditing 기본 사용자 값은 `security.system-user-id`를 사용합니다. 이전 키인 `dochiri.jpa.audit.system-user-id`도 하위 호환으로 읽습니다.

## starter가 제공하는 기능

### 시간

- 시스템 timestamp는 기본적으로 `Instant`를 사용합니다.
- 사용자에게 한국 시간으로 보여줄 필요가 있으면 `Asia/Seoul`로 변환해서 사용합니다.
- `Clock`은 자동 등록됩니다.

```java
@RestController
class TimeApi {

    private final Clock clock;

    TimeApi(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/api/public/ping")
    Map<String, Object> ping() {
        return Map.of("now", Instant.now(clock));
    }
}
```

### 예외 처리

에러 코드를 정의합니다.

```java
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

예외를 던집니다.

```java
throw new BaseException(UserErrorCode.USER_NOT_FOUND);
throw BaseException.of(UserErrorCode.USER_NOT_FOUND, "userId", userId);
```

전역 예외 처리를 활성화합니다.

```java
@RestControllerAdvice
public class ApiExceptionHandler extends GlobalExceptionHandler {
}
```

응답 예시:

```json
{
  "type": "/errors/user-not-found",
  "title": "USER_NOT_FOUND",
  "status": 404,
  "detail": "사용자를 찾을 수 없습니다.",
  "instance": "/api/users/1",
  "code": "USER_NOT_FOUND",
  "userId": 1
}
```

### Validation

`dochiri-api-starter`만 의존해도 바로 사용하실 수 있습니다.

```java
public record CreatePostRequest(
        @NotBlank String title
) {
}

@PostMapping("/api/public/posts")
void create(@Valid @RequestBody CreatePostRequest request) {
}
```

### JPA

엔티티는 `BaseEntity`를 상속하고, 식별자는 엔티티가 직접 선언합니다.

```java
@Entity
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    protected Post() {
    }

    public Post(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
```

특징:

- `BaseEntity`는 `createdAt`, `updatedAt`, `createdBy`, `updatedBy`만 제공합니다.
- 식별자 필드와 생성 전략은 각 엔티티가 직접 정합니다.
- `createdBy`, `updatedBy`는 `Long`
- `JpaRepository.delete*` 계열 메서드는 기본 JPA 동작대로 물리 삭제합니다.
- `JPAQueryFactory`는 자동 등록됩니다.
- 미인증 요청은 `security.system-user-id`를 감사자 값으로 사용합니다.

soft delete는 공통 모듈이 강제하지 않습니다.
필요하시면 소비 프로젝트에서 엔티티 필드, 조회 조건, 삭제 정책을 직접 정의하시는 편이 맞습니다.

Querydsl 사용:

`JPAQueryFactory` 빈은 자동 등록되지만, Q 클래스 생성용 annotation processor는 소비 프로젝트에서 직접 추가하셔야 합니다.
이 설정은 transitive dependency로 전달되지 않습니다.

소비 프로젝트 `build.gradle` 예시:

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-api-starter:0.0.1-SNAPSHOT'

    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
}
```

사용 예시:

```java
@Repository
class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    PostQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    List<Post> findRecent() {
        QPost post = QPost.post;

        return queryFactory
                .selectFrom(post)
                .orderBy(post.createdAt.desc())
                .fetch();
    }
}
```

### Security

기본 사용 경로는 `RefreshTokenService`로 access/refresh token을 함께 발급하고 refresh token을 DB에 저장하는 방식입니다.

중요한 계약:

- `dochiri-security-jpa`는 `User` 엔티티를 제공하지 않습니다.
- 사용자 도메인 모델과 로그인 비즈니스는 소비 프로젝트가 직접 정의합니다.
- `security-jpa`는 `refresh_tokens` 테이블과 `userId` 기반 refresh token 저장만 담당합니다.
- 따라서 `RefreshToken`은 `@ManyToOne User` 연관관계 대신 `Long userId`만 저장합니다.
- refresh token은 삭제보다 폐기 개념을 사용하므로 `deletedAt`이 아니라 `revokedAt`을 가집니다.
- 즉 refresh token 레코드는 남겨 두고, 더 이상 사용할 수 없게 된 시점을 `revokedAt`으로 기록합니다.

```java
@Service
class AuthService {

    private final RefreshTokenService refreshTokenService;

    AuthService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    JwtTokenResult login(Long userId, String role) {
        return refreshTokenService.generateToken(userId, role);
    }
}
```

refresh token 재발급 예시입니다.

```java
@Service
class TokenRefreshService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    TokenRefreshService(
            RefreshTokenService refreshTokenService,
            UserRepository userRepository
    ) {
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    JwtTokenResult refresh(String refreshToken) {
        Long userId = refreshTokenService.verifyAndExtractUserId(refreshToken);
        User user = userRepository.findById(userId).orElseThrow();

        refreshTokenService.revoke(refreshToken);
        return refreshTokenService.generateToken(user.getId(), user.getRole());
    }
}
```

로그아웃 예시입니다.

```java
@Service
class LogoutService {

    private final RefreshTokenService refreshTokenService;

    LogoutService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
```

전체 로그아웃 예시입니다.

```java
@Service
class LogoutAllService {

    private final RefreshTokenService refreshTokenService;

    LogoutAllService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    int logoutAll(Long userId) {
        return refreshTokenService.revokeAllByUserId(userId);
    }
}
```

인증 사용자 조회 예시입니다.

```java
@GetMapping("/api/me")
Map<String, Object> me(@AuthenticationPrincipal JwtPrincipal principal) {
    return Map.of(
            "userId", principal.userId(),
            "role", principal.role()
    );
}
```

`JwtTokenResult`는 아래 값을 반환합니다.

- `accessToken`
- `refreshToken`
- `refreshTokenExpiresAt`

`refreshTokenExpiresAt`은 `Instant`입니다.

starter에는 아래 항목도 포함됩니다.

- `RefreshToken` 엔티티
- `RefreshTokenRepository`
- `RefreshTokenService`

즉 서버가 refresh token을 DB에 기억하는 구조를 바로 사용하실 수 있습니다. 기본 테이블 이름은 `refresh_tokens`입니다.
기본 컬럼 의미는 아래와 같습니다.

- `user_id`: refresh token 소유 사용자 식별자
- `token_id`: JWT `jti`
- `expires_at`: refresh token 만료 시각
- `revoked_at`: refresh token 폐기 시각입니다. `null`이면 아직 폐기되지 않은 상태입니다.

`revokedAt`을 쓰는 이유는 아래와 같습니다.

- 로그아웃, 재발급, 강제 만료 같은 무효화 시점을 기록할 수 있습니다.
- refresh token 레코드를 바로 지우지 않아도 "왜 더 이상 못 쓰는지"를 표현할 수 있습니다.
- 나중에 만료되거나 오래된 폐기 토큰을 별도 배치로 물리 삭제하는 것도 가능합니다.

대신 아래 항목은 포함되지 않습니다.

- `User` 엔티티
- `UserRepository`
- 로그인/회원가입/사용자 조회 같은 사용자 도메인 로직

stateless 방식이 필요하시면 `JwtTokenGenerator`, `RefreshTokenVerifier`를 직접 사용하셔도 됩니다.

공개 경로는 아래 설정으로 제어합니다.

```yaml
security:
  public-endpoints:
    - /api/public/**
    - /api/auth/**
```

이 외 경로는 기본적으로 인증이 필요합니다.

401 응답 예시:

```json
{
  "type": "/errors/unauthorized",
  "title": "UNAUTHORIZED",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/me"
}
```

## 소비 프로젝트 검증 체크리스트

실제 개인 소비 프로젝트에서는 최소 아래 항목을 확인해 보시기를 권장합니다.

1. `./gradlew bootRun`으로 앱 기동
2. 공개 엔드포인트에서 `Clock` 주입 확인
3. `BaseException` 응답 확인
4. `BaseEntity` 저장 후 `createdAt`, `createdBy` 확인
5. JWT 발급 확인
6. `Authorization: Bearer <token>`으로 보호 엔드포인트 접근 확인

실제 검증 예시:

```bash
curl http://localhost:8080/api/public/ping
curl http://localhost:8080/api/public/error
curl -X POST http://localhost:8080/api/public/posts -H 'Content-Type: application/json' -d '{"title":"hello"}'
curl -X POST http://localhost:8080/api/auth/token
curl http://localhost:8080/api/me -H 'Authorization: Bearer <accessToken>'
```

## 개별 모듈 직접 사용

기본 경로는 starter 사용이지만, 필요하시면 개별 모듈만 직접 의존하실 수도 있습니다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-error-handling:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-time:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-jpa:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-security:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-security-jpa:0.0.1-SNAPSHOT'
}
```

이 경로는 모듈 조합 책임이 소비자에게 생기므로, 특별한 이유가 없으면 권장하지 않습니다.
