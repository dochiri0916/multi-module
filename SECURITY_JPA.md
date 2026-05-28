# Security JPA Module

`modules:security-jpa`는 refresh token을 DB에 저장하고 검증/폐기하는 기능을 제공하는 모듈이다.

`User` 엔티티나 사용자 도메인은 제공하지 않고, refresh token 소유자를 `userId` 값으로만 저장한다.

## 현재 구현

- `RefreshToken`
  - table: `refresh_tokens`
  - `userId`
  - `tokenId`
  - `expiresAt`
  - `revokedAt`
  - `BaseEntity` 상속
- `RefreshTokenRepository`
  - `findByTokenId`
  - `findByUserIdAndRevokedAtIsNull`
- `RefreshTokenService`
  - access/refresh token 발급
  - refresh token 저장
  - refresh token 검증
  - 단건 폐기
  - 사용자 전체 refresh token 폐기
- `SecurityJpaAutoConfiguration`
  - `RefreshTokenService` bean 등록
  - `SecurityJpaPackageRegistrar` import
- `SecurityJpaPackageRegistrar`
  - `RefreshToken` entity package와 repository package를 auto-configuration package에 등록

## 문제점

### 1. refresh token 저장 정책이 고정되어 있음

현재는 token 발급 시 항상 refresh token을 저장한다. 일부 서비스는 stateless refresh token, Redis 저장, 외부 세션 저장소를 선택할 수 있다.

### 2. 동시 재발급 정책이 없음

refresh token 재발급 시 기존 token을 폐기하고 새 token을 저장하는 흐름은 소비 서비스가 직접 조합해야 한다. rotate 정책을 모듈에서 제공할지 정해야 한다.

### 3. 오래된 token 정리 기능이 없음

만료되거나 폐기된 refresh token을 삭제하는 repository/service API가 없다. 운영 DB에서는 정리 배치가 필요하다.

### 4. repository 조회가 active token 기준으로 충분하지 않음

현재 `findByUserIdAndRevokedAtIsNull`는 만료 여부를 DB 조건에 포함하지 않는다. service에서 `isExpired`를 볼 수 있지만, 대량 데이터에서는 DB 조건이 필요할 수 있다.

### 5. token id unique 충돌 처리 정책이 없음

`token_id`는 unique다. UUID 충돌 가능성은 낮지만, 저장 실패 시 재시도 정책은 없다.

### 6. auditing 타입 이슈를 JPA 모듈과 공유함

`RefreshToken`이 `BaseEntity`를 상속하므로 JPA auditing 타입 정책의 영향을 그대로 받는다.

## 리팩토링 방향

### 1단계: 저장소 책임 명확화

`RefreshTokenService`가 담당하는 범위를 문서화한다.

현재 책임:

- token 생성은 `JwtTokenGenerator`
- refresh token claim 검증은 `JwtProvider`
- 저장/조회/폐기는 `RefreshTokenRepository`
- 서비스는 위 기능을 조합

검토:

- `RefreshTokenStore` interface를 만들고 JPA 구현을 제공할지
- 지금은 JPA 전용 모듈이므로 interface 없이 단순 유지할지

권장:

- 지금은 단순 유지
- Redis나 외부 저장소 요구가 생기면 `security-token-store` 같은 별도 추상화 검토

### 2단계: refresh token rotation API 추가

소비 서비스가 매번 직접 구현하지 않도록 아래 API를 검토한다.

```java
@Transactional
public JwtTokenResult rotate(String refreshToken, String role) {
    VerifiedRefreshToken verified = verify(refreshToken);
    revoke(refreshToken);
    return generateToken(verified.userId(), role);
}
```

주의:

- role을 refresh token claim에서 그대로 쓸지
- 최신 사용자 role을 소비 서비스가 조회해서 넘길지 결정해야 한다.

권장:

- 보안상 최신 role은 소비 서비스가 사용자 DB에서 조회한 뒤 넘긴다.
- 모듈은 `verify`, `revoke`, `generateToken` 조합 API만 제공한다.

### 3단계: cleanup API 추가

repository 메서드 후보:

```java
int deleteByExpiresAtBefore(Instant now);
int deleteByRevokedAtIsNotNullAndRevokedAtBefore(Instant threshold);
```

service 메서드 후보:

```java
int deleteExpiredTokens();
int deleteRevokedTokensBefore(Instant threshold);
```

목표:

- 소비 서비스가 스케줄러에서 쉽게 호출할 수 있다.
- 모듈이 스케줄러를 자동 등록하지는 않는다.

### 4단계: active token 조회 최적화

추가 repository 후보:

```java
List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(Long userId, Instant now);
```

`revokeAllByUserId`는 만료 token까지 폐기 표시할 필요가 있는지 정책을 정한다.

### 5단계: transactional 경계와 lock 정책 검토

동시에 같은 refresh token으로 재발급 요청이 들어올 수 있다.

검토안:

- token row 조회 시 pessimistic lock
- unique token id와 revokedAt update로 idempotent 처리
- 재발급 API에서 기존 token revoke와 새 token 저장을 같은 transaction으로 묶기

초기 권장:

- 단건 verify/revoke/generate API는 유지
- rotate API를 추가한다면 같은 transaction 안에서 처리
- 고부하 서비스에서 lock 요구가 생기면 별도 lock repository method 추가

### 6단계: 테스트 보강

테스트 대상:

- token 발급 시 refresh token 저장
- 저장되지 않은 token 거부
- user id 불일치 거부
- 만료 token 거부
- revoked token 거부
- 단건 revoke idempotency
- 사용자 전체 revoke
- cleanup API
- rotation API

## 사용 예시

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.dochiri:dochiri-security-jpa:0.0.1-SNAPSHOT'
}
```

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

refresh token 재발급은 소비 서비스가 최신 사용자 상태를 확인한 뒤 조합한다.

```java
JwtTokenResult refresh(String refreshToken) {
    Long userId = refreshTokenService.verifyAndExtractUserId(refreshToken);
    User user = userRepository.findById(userId).orElseThrow();

    refreshTokenService.revoke(refreshToken);
    return refreshTokenService.generateToken(user.getId(), user.getRole());
}
```

## 완료 기준

- refresh token 저장/검증/폐기 책임이 명확하다.
- 재발급과 cleanup에 필요한 service API가 정리되어 있다.
- 동시 재발급 정책이 문서화되어 있다.
- JPA auditing 타입 정책과 충돌하지 않는다.
- `./gradlew :modules:security-jpa:test`가 통과한다.
