# Security Module

`modules:security`는 JWT 발급/검증, Spring Security filter chain, CORS, 인증 실패 응답, auditing 연동을 제공하는 모듈이다.

## 현재 구현

- `SecurityAutoConfiguration`
  - `JwtAutoConfiguration`
  - `SecurityFilterChainAutoConfiguration`
  - `CorsAutoConfiguration`
  - `SecurityAuditAutoConfiguration`
- JWT
  - `JwtProvider`
  - `JwtTokenGenerator`
  - `RefreshTokenVerifier`
  - `JwtAuthenticationConverter`
  - `JwtAuthenticationFilter`
  - `JwtPrincipal`
- Security response
  - `JwtAuthenticationEntryPoint`
  - `JwtAccessDeniedHandler`
  - `SecurityResponseWriter`
- Properties
  - `jwt.secret`
  - `jwt.access-expiration`
  - `jwt.refresh-expiration`
  - `cors.allowed-origins`
  - `security.public-endpoints`
  - `security.system-user-id`

## 문제점

### 1. security filter chain 자동 등록의 영향이 큼

`dochiri-security`를 의존하면 기본 `SecurityFilterChain`이 등록된다. 서비스가 자체 security 정책을 갖는 경우에는 `SecurityFilterChain` bean을 직접 등록하면 기본 bean은 물러나지만, 이 정책을 문서로 분명히 해야 한다.

### 2. JWT 설정 단위가 millisecond long 값임

`accessExpiration`, `refreshExpiration`이 `long` millisecond다. 설정 파일에서 의미가 덜 명확하고 실수하기 쉽다.

개선 방향:

- `Duration` 타입으로 전환
- 하위 호환이 필요하면 기존 long millisecond를 일정 기간 지원

### 3. Security 응답 계약이 error-handling 응답과 다름

`SecurityResponseWriter`는 `ProblemDetail`을 쓰지만 `code`, `traceId`를 넣지 않는다. error-handling 모듈의 응답 계약과 완전히 같지는 않다.

정해야 할 것:

- security 모듈이 error-handling 모듈에 의존할지
- security 응답에도 자체적으로 `code`, `traceId`를 넣을지
- 두 모듈을 독립적으로 유지하되 문서상 응답 차이를 허용할지

### 4. JWT claim 계약이 고정되어 있음

현재 claim:

- `sub`: user id
- `role`
- `category`: `access` 또는 `refresh`
- refresh token은 `jti` 사용

MSA에서 서비스별 tenant, organization, permission 같은 확장 claim이 필요할 수 있다.

### 5. `JwtAuthenticationFilter`가 인증 실패를 조용히 넘김

토큰이 잘못되면 security context를 비우고 filter chain을 계속 진행한다. 이후 보호 엔드포인트에서 401이 발생하므로 동작은 자연스럽지만, 원인별 응답 메시지를 구분하기는 어렵다.

### 6. auditor 타입을 JPA 모듈과 맞춤

`SecurityAuditAutoConfiguration`은 `AuditorAware<String>`를 제공한다. `jpa` 모듈의 `BaseEntity`도 `String createdBy/updatedBy`를 사용하므로 auditing 저장 타입은 문자열로 통일한다.

## 리팩토링 방향

### 1단계: 설정 계약 정리

`JwtProperties`를 `Duration` 기반으로 바꾸는 것을 검토한다.

목표 설정:

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-ttl: 1h
  refresh-token-ttl: 7d
```

하위 호환:

- 기존 `access-expiration`, `refresh-expiration`은 deprecated 문서화
- 내부 변환으로 일정 기간 지원

### 2단계: Security 응답 계약 정렬

권장안:

- security 모듈은 error-handling 모듈에 직접 의존하지 않는다.
- 대신 `SecurityResponseWriter`가 `code`, `traceId`를 직접 추가한다.

예시:

```json
{
  "type": "/errors/unauthorized",
  "title": "UNAUTHORIZED",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/me",
  "code": "UNAUTHORIZED",
  "traceId": "..."
}
```

### 3단계: JWT claim 확장 지점 추가

검토안:

- `JwtClaimsCustomizer` interface 제공
- token 생성 시 추가 claims를 받을 수 있는 overload 추가
- 기본 claim 이름은 상수로 공개하거나 문서화

주의:

- refresh token 저장 모듈은 `jti`, `sub`, 만료 시각에 의존하므로 이 계약은 깨면 안 된다.

### 4단계: SecurityFilterChain 자동 등록 조건 문서화 및 보강

현재 `@ConditionalOnMissingBean(SecurityFilterChain.class)` 정책은 유지한다.

보강할 것:

- 소비 프로젝트가 직접 `SecurityFilterChain`을 등록하면 기본 filter chain은 등록되지 않음
- public endpoint 기본값과 추가 방식 문서화
- Swagger 경로 기본 포함 여부를 모듈 목표에 맞게 재검토

### 5단계: auditor 타입 정렬

상태: 완료

JPA 모듈 방향과 맞춰 `AuditorAware<String>`로 통일한다.

반영 내용:

- `SecurityAuditorAware`가 `String` 반환
- `systemUserId`도 문자열 저장 기준으로 정리

### 6단계: 테스트 보강

테스트 대상:

- JWT access/refresh 발급과 검증
- 만료 토큰
- access/refresh category 혼용 방지
- 잘못된 role, subject, jti
- 401/403 응답의 `ProblemDetail` 계약
- custom `SecurityFilterChain` 등록 시 기본 filter chain 미등록
- CORS wildcard와 credentials 정책

## 사용 예시

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'com.dochiri:dochiri-security:0.0.1-SNAPSHOT'
}
```

```yaml
jwt:
  secret: test-secret-key-that-is-at-least-32-characters-long
  access-expiration: 3600000
  refresh-expiration: 604800000

security:
  public-endpoints:
    - /api/public/**
    - /api/auth/**
  system-user-id: 0
```

## 완료 기준

- JWT 설정 단위가 명확하다.
- security 응답 계약이 error-handling 응답과 어긋나지 않는다.
- 소비 프로젝트가 기본 filter chain을 쉽게 대체할 수 있다.
- JPA auditing 타입과 충돌하지 않는다.
- `./gradlew :modules:security:test`가 통과한다.
