# Security Modules

보안 기능은 순수 Domain/Application과 Gateway 검증, 인증 서버 발급/JPA Adapter로 분리한다. 소비 프로젝트는 내부 보안 모듈을 직접 조합하지 않고 역할별 공개 starter를 사용한다.

## 모듈 책임

| 모듈 | 책임 |
| --- | --- |
| `security-domain` | `RefreshSession` Aggregate, `AuthenticationSubject`, `AuthenticationRole`, token/session VO, Domain 예외 |
| `security-application` | 발급·검증·회전·폐기·정리 UseCase와 token codec·repository·시간·ID Port |
| `security-error-webmvc` | 보안 Application 예외를 RFC 9457 ProblemDetail로 변환하는 Context Advice |
| `security-jwt` | Gateway용 JJWT `AccessTokenVerifierPort` 구현 |
| `security-jwt-issuer` | 인증 서버용 발급·회전·refresh 검증·시간·token ID Port 구현 |
| `security-webmvc` | 인증 filter/principal, `@PublicApi`, CORS/filter chain, 공통 401/403 Adapter |
| `msa-gateway-starter` | Gateway 검증 조합. DB와 발급 기능 없음 |
| `msa-auth-starter` | 인증 서버 발급·refresh session·MySQL 조합. Access Token filter 없음 |

```text
security-domain <- security-application <- security-jwt             (access 검증)
                                      \--- security-jwt-issuer      (발급/refresh 검증)
                                      \--- security-error-webmvc   (Context Advice)
                                      \--- security-webmvc ------> security-error-webmvc
```

`msa-gateway-starter` runtime classpath에는 Spring Data와 JPA가 없다. refresh token 저장과 Security auditing은 `msa-auth-starter`에만 포함된다.

## 공개 Domain 계약

- `AuthenticationSubject(String)`: 소비 Context 식별자의 인증 표현
- `AuthenticationRole(String)`: `ROLE_` prefix를 제거해 정규화한 role
- `EncodedToken(String)`: raw token을 캡슐화하고 문자열 표현에서 값을 가린다.
- `TokenId(String)`: token 식별자. `generate()`는 하이픈 없는 UUID를 생성한다.
- `RefreshSessionId`: rotation 동안 유지되는 세션 식별자
- `RefreshSession`: session ID만으로 동등성을 판단하고 현재 token과 폐기 상태를 보호하는 불변 Aggregate

공통 라이브러리는 소비 Context의 `MemberId`를 알지 못한다. 소비 경계에서 명시적으로 변환한다.

```java
AuthenticationSubject subject = new AuthenticationSubject(memberId.value());
```

## Application UseCase

| Inbound Port | 입력 | 출력 | 트랜잭션 |
| --- | --- | --- | --- |
| `IssueTokensUseCase` | `IssueTokensCommand` | `IssueTokensResult` | 변경 |
| `VerifyRefreshTokenUseCase` | `VerifyRefreshTokenQuery` | `VerifyRefreshTokenResult` | 읽기 전용 |
| `RotateTokensUseCase` | `RotateTokensCommand` | `RotateTokensResult` | 변경 |
| `RevokeRefreshTokenUseCase` | `RevokeRefreshTokenCommand` | `RevokeRefreshTokenResult` | 변경 |
| `RevokeAllRefreshTokensUseCase` | `RevokeAllRefreshTokensCommand` | `RevokeAllRefreshTokensResult` | 변경 |
| `CleanupRefreshSessionsUseCase` | `CleanupRefreshSessionsCommand` | `CleanupRefreshSessionsResult` | 변경 |

Application 계층은 다음 Outbound Port 계약을 소유한다.

- `AccessTokenVerifierPort`
- `TokenIssuerPort`, `RotatingTokenIssuerPort`
- `RefreshTokenVerifierPort`, `RefreshSessionTokenVerifierPort`
- `TokenIdGeneratorPort`
- `CurrentTimePort`
- `RefreshSessionPort`
- `RefreshSessionBulkRevocationPort`
- `RefreshSessionCleanupPort`

JJWT, JPA Entity, Spring Data Repository, Spring Security 예외는 Application 공개 계약에 나타나지 않는다.

## JWT Adapter 경계

`JjwtAccessTokenVerifierAdapter`는 Gateway에서 access token만 검증한다. `JjwtTokenIssuerAdapter`와 `JjwtRefreshTokenVerifierAdapter`는 인증 서버에만 존재한다. 세 Adapter는 다음 claim 계약을 공유한다.

| claim | access | refresh |
| --- | --- | --- |
| `sub` | `AuthenticationSubject.value()` | 동일 |
| `role` | 정규화된 role | 동일 |
| `category` | `access` | `refresh` |
| `jti` | 없음 | `TokenId.value()` |
| `sid` | 없음 | `RefreshSessionId.value()` |
| `exp` | access TTL | refresh TTL |

최초 발급에서는 `sid`와 `jti`가 같은 값이고, rotation 뒤에는 `sid`만 유지되며 `jti`가 교체된다. refresh 만료 시각은 최초 발급 시점의 절대 만료를 유지해 rotation으로 세션 수명이 연장되지 않는다.

JJWT `Claims`와 SDK 예외는 Adapter 내부에만 존재한다. 검증 결과는 자체 record로 변환하고, 실패는 `InvalidTokenException`의 Application error code로 변환한다.

JWT 검증은 주입된 `Clock`을 사용한다. 소비자가 역할별 Port, `Clock`, `TokenIdGeneratorPort`, `CurrentTimePort`를 제공하면 해당 기본 Adapter가 물러난다.

## 설정

Gateway의 필수 설정은 `JWT_SECRET` 하나다. 인증 서버는 같은 `JWT_SECRET`과 인증 DB 정보를 제공한다. 일반 서비스에는 JWT 설정을 배포하지 않는다. Spring Boot가 `jwt.secret`으로 자동 바인딩하며 TTL은 인증 서버 정책을 바꿀 때만 선언한다.

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-ttl: 1h
  refresh-token-ttl: 7d

cors:
  allowed-origins:
    - https://app.example.com

security:
  swagger-public: false
```

- secret은 최소 32자다.
- access TTL 기본값 `1h`와 refresh TTL 기본값 `7d`는 `security-jwt-issuer`에만 적용된다. 재정의할 때는 0보다 큰 Spring `Duration` 형식이어야 한다.
- wildcard CORS origin을 사용하면 credentials는 자동으로 비활성화된다.
- Swagger endpoint는 기본적으로 보호되며 `security.swagger-public=true`일 때만 공개된다.

## 공개 API

Gateway가 Servlet Handler 기반 endpoint를 직접 제공할 때 공개 endpoint는 path 설정 목록이 아니라 handler metadata로 표시한다.

```java
@PublicApi
@PostMapping("/api/auth/login")
LoginResponse login(@RequestBody LoginRequest request) {
    return loginUseCase.execute(request.toCommand());
}
```

`@PublicApi`는 type과 method에 사용할 수 있다. `PublicApiRequestMatcher`는 실제 `HandlerMethod`의 metadata를 확인하며, handler 조회 실패나 알 수 없는 handler를 공개 권한으로 승격하지 않는다.

## 인증 principal

유효한 access token은 다음 principal로 변환된다.

```java
public record JwtPrincipal(
        AuthenticationSubject subject,
        AuthenticationRole role
) implements Principal {
}
```

`JwtPrincipal.getName()`은 subject 문자열을 반환한다. `Long userId` 계약은 제공하지 않는다.

## 401/403 오류 계약

`security-error-webmvc`는 보안 Application 예외를 Spring 공식 `ProblemDetail`로 변환하는 Context Advice만 제공한다. 따라서 인증 서버는 refresh 오류를 HTTP 응답으로 변환하면서도 JWT filter나 `SecurityFilterChain`을 받지 않는다. Gateway의 Spring Security handler는 `SecurityErrorResponsePort`만 호출하고, 기본 `SecurityProblemDetailResponseAdapter`가 표준 필드를 직접 작성한다.

| 상황 | status | type |
| --- | ---: | --- |
| 인증 필요 | 401 | `/problems/authentication-required` |
| 접근 거부 | 403 | `/problems/access-denied` |

```json
{
  "type": "/problems/authentication-required",
  "title": "인증 필요",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/me"
}
```

내부 예외 메시지와 raw token은 응답에 노출하지 않는다. 애플리케이션의 Jackson `ObjectMapper`를 주입받으며 별도 전역 mapper를 생성하지 않는다.

## 자동 구성 back-off

기본 구성은 필요한 classpath와 Bean이 있을 때만 활성화된다.

- Gateway의 JWT 설정과 `AccessTokenVerifierPort`
- 인증 서버의 issuer/refresh verifier, token ID generator와 current time Port
- 필터와 독립적인 보안 Application 예외 처리 Advice
- JWT converter/filter
- 보안 exception handler와 filter response Adapter
- `SecurityFilterChain`
- `CorsConfigurationSource`

소비자가 동일 역할 Bean을 등록하면 기본 Bean은 생성되지 않는다.

`security-jwt-issuer`와 `security-jpa`가 함께 있으면 기본 발급·refresh 검증·폐기 UseCase가 연결된다. `RotatingTokenIssuerPort`와 `RefreshSessionTokenVerifierPort`가 있으면 rotation이, `RefreshSessionCleanupPort`가 있으면 cleanup이 각각 조건부로 추가된다.

## 사용

API Gateway:

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-msa-gateway-starter:1.0.0'
}
```

인증 서버:

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-msa-auth-starter:1.0.0'
}
```

`security-jwt`, `security-webmvc`, `security-jwt-issuer`, `security-jpa`는 두 starter를 구성하는 내부 artifact이며 소비 프로젝트가 직접 선택하지 않는다.

## 마이그레이션

| 제거된 계약 | 대체 계약 |
| --- | --- |
| `JwtProvider`, `JwtTokenGenerator` | 역할별 token Port와 `IssueTokensUseCase` |
| public `Claims` | `DecodedAccessToken`, `DecodedRefreshToken` |
| token별 저장 모델 | `RefreshSession`과 안정적인 `sid` |
| `Long userId` | `AuthenticationSubject(String)` |
| `security.public-endpoints` | `@PublicApi` |
| Swagger 항상 공개 | `security.swagger-public=false` 기본값 |
| millisecond expiration | `jwt.*-token-ttl` `Duration` |
| 별도 `SecurityResponseWriter` | `SecurityErrorResponsePort`와 공통 ProblemDetail factory |

## 검증

```bash
./gradlew :modules:security-domain:test
./gradlew :modules:security-application:test
./gradlew :modules:security-jwt:test
./gradlew :modules:security-jwt-issuer:test
./gradlew :modules:security-webmvc:test
./gradlew :modules:msa-gateway-starter:test
./gradlew :modules:msa-auth-starter:test
```

소비자 스모크 테스트는 Spring Data가 없는 classpath에서 context 기동, `@PublicApi`, JWT 인증, 문자열 subject, 401/403 공통 계약, Swagger 기본 비공개를 검증한다.
