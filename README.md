# Multi-Module Project (Common Libraries)

이 프로젝트는 **Dochiri** 서비스 생태계에서 사용되는 공통 기능을 모듈화하여 관리하는 저장소입니다.
각 모듈은 독립적인 JAR 라이브러리로 빌드되어 다른 애플리케이션(API, Batch 등)에서 의존성으로 추가하여 사용됩니다.

## 🛠 Tech Stack

- **Java**: 25
- **Build Tool**: Gradle
- **Framework**: Spring Boot 4.0.3 (Dependency Management)
- **ORM**: JPA / QueryDSL 5.1.0
- **Security**: Spring Security / JJWT 0.12.6

## 📂 Module Structure

이 프로젝트는 공통 모듈만 관리하며, 실제 비즈니스 로직을 수행하는 애플리케이션은 포함하지 않습니다.

| 모듈명 | artifactId | 설명 | 주요 의존성 |
|:---:|:---|:---|:---|
| **common** | `dochiri-common` | 예외 처리(ErrorCode, BaseException), Clock 설정 | `spring-context` |
| **jpa** | `dochiri-jpa` | JPA Auditing, BaseEntity, QueryDSL 설정 | `common`, `spring-data-jpa`, `querydsl` |
| **security** | `dochiri-security` | JWT 인증/인가, 보안 설정, 토큰 Provider | `common`, `spring-security`, `jjwt` |

## 🚀 Getting Started

### 1. Build Modules

루트 디렉토리에서 전체 모듈을 빌드합니다.

```bash
./gradlew clean build
```

### 2. Publish to Local Maven (로컬 개발용)

로컬 환경에서 다른 프로젝트가 이 모듈들을 사용할 수 있도록 로컬 Maven 저장소(`~/.m2/repository`)에 배포합니다.

```bash
./gradlew publishToMavenLocal
```

## 📦 How to Use (in other projects)

다른 Spring Boot 프로젝트에서 이 모듈을 사용하려면 `build.gradle`에 아래와 같이 추가합니다.

### settings.gradle

```gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal() // 로컬 빌드된 스냅샷 버전을 가져오기 위해 필요
    }
}
```

### build.gradle

```gradle
dependencies {
    // 필요한 모듈만 선택하여 추가
    implementation 'com.dochiri:dochiri-common:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-jpa:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-security:0.0.1-SNAPSHOT'
}
```

---

## ⚙️ Module Details

### Common Module (`dochiri-common`)

예외 처리 프레임워크와 공통 설정을 제공합니다. 에러 응답은 RFC 9457 (Problem Details) 표준을 따릅니다.

#### ErrorCode 정의

각 애플리케이션에서 `ErrorCode` 인터페이스를 구현하여 자체 에러코드를 정의합니다.

```java
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
```

#### 예외 발생

```java
// 기본 사용
throw new BaseException(UserErrorCode.USER_NOT_FOUND);

// 추가 속성 포함
throw BaseException.of(UserErrorCode.USER_NOT_FOUND, "userId", userId);

// Map으로 전달
throw new BaseException(UserErrorCode.DUPLICATE_EMAIL, Map.of("email", email));
```

#### 응답 예시 (RFC 9457)

```json
{
  "type": "/errors/user-not-found",
  "title": "USER_NOT_FOUND",
  "status": 404,
  "detail": "사용자를 찾을 수 없습니다.",
  "instance": "/api/users/123",
  "code": "USER_NOT_FOUND",
  "userId": 123
}
```

#### GlobalExceptionHandler 활성화

`GlobalExceptionHandler`는 라이브러리 모듈이므로 직접 활성화되지 않습니다.
사용처에서 상속받아 `@RestControllerAdvice`를 붙여 활성화합니다.

```java
@RestControllerAdvice
public class ApiExceptionHandler extends GlobalExceptionHandler {
}
```

이것만으로 다음이 자동 처리됩니다:
- `BaseException` → RFC 9457 형식 응답
- Spring MVC 표준 예외 (검증 실패, 잘못된 HTTP 메서드 등) → ProblemDetail 형식 응답
- 미처리 예외 → 500 에러 + 로깅

---

### JPA Module (`dochiri-jpa`)

- **BaseEntity**: 생성일, 수정일, 생성자, 수정자를 자동으로 관리하는 매핑 상위 클래스를 제공합니다. Soft delete(`deletedAt`)를 지원합니다.
- **QueryDSL**: `JPAQueryFactory` 빈이 자동 등록되어, 별도의 QueryDSL 설정 없이 바로 사용 가능합니다.

---

### Security Module (`dochiri-security`)

- **JWT**: `JwtTokenGenerator`, `JwtAuthenticationFilter`를 통해 토큰 기반 인증을 제공합니다.
- **SecurityConfig**: 기본 보안 설정을 제공하며, 필요 시 사용자 정의 가능합니다.

#### 필수 `application.yml` 설정

```yaml
jwt:
  secret: "your-secret-key"        # JWT 서명에 사용할 비밀 키 (최소 32자)
  access-expiration: 3600000        # 액세스 토큰 만료 시간 (ms), 예: 1시간
  refresh-expiration: 604800000     # 리프레시 토큰 만료 시간 (ms), 예: 7일

cors:
  allowed-origins:
    - "https://example.com"         # CORS 허용할 origin 목록

security:
  public-endpoints:
    - "/api/auth/**"                # 인증 없이 접근 가능한 엔드포인트 패턴 목록
    - "/api/public/**"
```

> IDE(IntelliJ 등)에서 `application.yml` 작성 시 자동완성이 지원됩니다.

---

## 💡 Usage Examples

### 1. 로그인 - 토큰 발급

`JwtTokenGenerator`를 주입받아 로그인 성공 시 토큰을 발급합니다.
`JwtTokenResult`에는 `accessToken`, `refreshToken`, `refreshTokenExpiresAt`이 담겨 있습니다.

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenGenerator jwtTokenGenerator;

    public JwtTokenResult login(String email, String password) {
        // 사용자 인증 로직 (생략)
        Long userId = user.getId();
        String role = user.getRole(); // 예: "USER", "ADMIN"

        return jwtTokenGenerator.generateToken(userId, role);
    }
}
```

---

### 2. 리프레시 토큰 재발급

`RefreshTokenVerifier`를 주입받아 리프레시 토큰을 검증하고 새 액세스 토큰을 발급합니다.

```java
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RefreshTokenVerifier refreshTokenVerifier;
    private final JwtTokenGenerator jwtTokenGenerator;

    public String refresh(String refreshToken) {
        // 유효하지 않은 리프레시 토큰이면 BadCredentialsException 발생
        Long userId = refreshTokenVerifier.verifyAndExtractUserId(refreshToken);

        String role = userRepository.findById(userId).getRole();
        return jwtTokenGenerator.generateAccessToken(userId, role);
    }
}
```

---

### 3. 현재 로그인 사용자 정보 조회

필터를 통과한 요청의 `SecurityContext`에서 `JwtPrincipal`을 꺼냅니다.

```java
@GetMapping("/me")
public ResponseEntity<?> getMe(@AuthenticationPrincipal JwtPrincipal principal) {
    Long userId = principal.userId();
    String role = principal.role();
    // ...
}
```

---

### 4. SecurityFilterChain 커스터마이징

기본 제공되는 `SecurityFilterChain`이 맞지 않을 경우, 직접 정의하면 자동 설정이 비활성화됩니다.
`JwtAuthenticationFilter` 등 모듈에서 등록된 빈은 그대로 주입받아 사용할 수 있습니다.

```java
@Configuration
@RequiredArgsConstructor
public class CustomSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

---
**Note**: 상세한 설계 의도와 배포 전략은 `COMMON_MODULE_DESIGN.md` 문서를 참고하세요.
