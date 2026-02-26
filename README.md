# Multi-Module Commerce Project

Spring Boot 기반 멀티 모듈 커머스 프로젝트

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle 9.3.1 |
| ORM | Spring Data JPA, QueryDSL 5.1.0 |
| Security | Spring Security, JJWT 0.12.6 |
| Batch | Spring Batch |
| DB | H2 (in-memory) |
| Docs | SpringDoc OpenAPI 2.8.9 |

## 모듈 구조

```
multi-module/
├── applications/
│   ├── commerce-api/       # REST API 애플리케이션
│   └── commerce-batch/     # 배치 애플리케이션
└── modules/
    ├── jpa/                # JPA 공통 모듈 (BaseEntity, QueryDSL)
    └── security/           # 인증/인가 공통 모듈 (JWT, Spring Security)
```

### commerce-api

쿠폰 도메인 중심의 REST API 서버. 레이어드 아키텍처 적용.

```
commerce-api/
├── domain/           # 엔티티, 도메인 리포지토리 인터페이스
│   ├── coupon/       # Coupon, CouponStatus, CouponRepository
│   └── user/         # User, UserRole
├── infrastructure/   # JPA 어댑터 (도메인 리포지토리 구현체)
├── application/      # 서비스 레이어 (비즈니스 로직)
└── presentation/     # REST 컨트롤러, Request/Response DTO
```

### commerce-batch

쿠폰 만료 처리 배치. Spring Batch + `@Scheduled` 기반.

### modules/jpa

- `BaseEntity`: 공통 감사(Audit) 필드 + Soft Delete
- `JpaSupportConfig`: JPA Auditing, JPAQueryFactory 빈 등록

### modules/security

- JWT 토큰 발급/검증 (`JwtTokenProvider`)
- 인증 필터 (`JwtAuthenticationFilter`)
- Spring Security 설정 (`SecurityConfig`)
- JPA Auditor 연동 (`CurrentUserAuditorAware`)

---

## 좋은 점 (Good)

### 1. 명확한 모듈 분리

`applications`과 `modules`를 분리하여 공통 관심사(JPA, Security)를 재사용 가능한 라이브러리 모듈로 추출한 점이 좋다. commerce-batch는 security 모듈 없이 jpa 모듈만 의존하는 등 필요한 모듈만 선택적으로 사용하고 있다.

### 2. 도메인 리포지토리 패턴 (Port-Adapter)

```
CouponRepository (domain 인터페이스)
    └── CouponJpaAdapter (infrastructure 구현체)
            └── CouponJpaRepository (Spring Data)
```

도메인 레이어가 Spring Data JPA에 직접 의존하지 않도록 인터페이스를 분리했다. 인프라 기술 교체 시 도메인 로직에 영향이 없는 구조.

### 3. 도메인 엔티티의 비즈니스 로직 캡슐화

`Coupon` 엔티티에 `register()` 정적 팩토리 메서드, `use()`, `isExpiredAt()` 등 비즈니스 규칙이 도메인 객체 안에 응집되어 있다. 서비스 레이어가 얇아지고, 도메인 로직 테스트가 용이한 구조.

### 4. BaseEntity를 통한 공통 관심사 통합

감사(Audit) 필드(`createdAt`, `updatedAt`, `createdBy`, `updatedBy`)와 Soft Delete(`deletedAt`)를 `BaseEntity`로 일관되게 관리하고 있다.

### 5. Use-Case 패턴 적용 (RegisterCouponService)

`Input/Output` 레코드를 활용한 Use-Case 패턴이 적용되어 있어 명확한 입출력 계약을 가진다.

### 6. JWT 인증의 모듈화

JWT 인증 로직이 독립 모듈로 분리되어 있어 다른 애플리케이션에서도 재사용 가능하다.

### 7. Spring Batch 배치 설계

배치 Job을 `Tasklet` 기반으로 구현하고, `@Scheduled`로 스케줄링하며, cron 표현식을 외부 프로퍼티로 설정 가능하게 한 점이 좋다.

---

## 리팩토링 포인트 (Refactoring)

### 1. AuthController가 security 모듈에 위치 - 관심사 분리 위반

**현재**: `modules/security`에 `AuthController`가 있고, 하드코딩된 인증 로직(`userId=1, password="password123!"`)이 포함되어 있다.

**문제**: 공통 라이브러리 모듈에 특정 비즈니스 로직(로그인)이 포함되면 재사용성이 떨어지고 모듈의 책임이 불명확해진다.

**개선안**: `AuthController`를 `commerce-api`의 presentation 레이어로 이동. security 모듈은 인증 인프라(JWT, Filter, Config)만 제공하도록 한다.

```
modules/security/      → JWT 인프라만 제공
commerce-api/
  └── presentation/auth/AuthController.java  → 인증 API
  └── application/auth/AuthService.java      → 인증 비즈니스 로직
```

### 2. CouponService와 RegisterCouponService의 책임 중복

**현재**: `CouponService.create()`와 `RegisterCouponService.execute()`가 동일한 쿠폰 생성 로직을 수행한다. 두 서비스가 공존하면 어디를 호출해야 하는지 혼란스럽다.

**개선안**: 하나의 패턴으로 통일한다. Use-Case 패턴(`RegisterCouponService`)으로 통일하거나, `CouponService` 하나로 합친다.

```java
// Option A: Use-Case 패턴 통일
RegisterCouponService  → 쿠폰 생성
UseCouponService       → 쿠폰 사용
GetCouponService       → 쿠폰 조회

// Option B: 하나의 서비스로 통합
CouponService.register()
CouponService.use()
CouponService.get()
```

### 3. CouponRepository 도메인 인터페이스에 findById 누락

**현재**: `CouponRepository`에는 `save()`와 `findByCode()`만 정의되어 있지만, `CouponService.get()`과 `use()`에서 ID 기반 조회가 필요하다. 현재 `CouponService`가 `CouponJpaRepository`를 직접 주입받아 사용 중일 가능성이 높다.

**개선안**: 도메인 리포지토리에 필요한 메서드를 모두 정의하고, 서비스는 도메인 인터페이스만 의존하도록 한다.

```java
public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long id);
    Optional<Coupon> findByCode(String code);
}
```

### 4. CouponIssuance 엔티티가 비어 있음

**현재**: `CouponIssuance` 엔티티가 `BaseEntity`만 상속하고 필드가 없다. 의도는 쿠폰-사용자 발급 이력 관리로 보이나 미구현 상태.

**개선안**: 구현 예정이 아니라면 삭제. 구현 예정이라면 최소한 연관관계를 정의한다.

```java
@Entity
public class CouponIssuance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private ZonedDateTime issuedAt;
}
```

### 5. User 도메인의 리포지토리 패턴 미적용

**현재**: `User` 엔티티는 존재하지만, `UserJpaRepository`(Spring Data)만 있고 도메인 리포지토리 인터페이스가 없다. `Coupon` 도메인에는 적용된 Port-Adapter 패턴이 `User`에는 미적용.

**개선안**: 일관성을 위해 동일한 패턴 적용.

```
domain/user/UserRepository.java              (도메인 인터페이스)
infrastructure/user/UserJpaAdapter.java       (어댑터)
infrastructure/user/UserJpaRepository.java    (Spring Data)
```

### 6. 예외 처리 전략 부재

**현재**: 도메인 로직에서 `IllegalStateException`, `IllegalArgumentException` 등 일반 예외를 사용하고 있으며, 글로벌 예외 핸들러(`@RestControllerAdvice`)가 없다.

**개선안**:

```java
// 1. 도메인 예외 정의
public class CouponAlreadyUsedException extends BusinessException { ... }
public class CouponExpiredException extends BusinessException { ... }
public class CouponNotFoundException extends BusinessException { ... }

// 2. 글로벌 예외 핸들러
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) { ... }
}
```

### 7. API 응답 형식 표준화

**현재**: 컨트롤러마다 응답 형식이 다르다. `CouponController`는 엔티티/DTO를 직접 반환하고, `AuthController`는 별도 record를 사용한다.

**개선안**: 공통 응답 래퍼를 도입한다.

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
}
```

### 8. Soft Delete 필터링 미적용

**현재**: `BaseEntity`에 `deletedAt` 필드가 있지만, 조회 시 삭제된 데이터를 자동 필터링하는 로직이 없다. 모든 조회 쿼리에서 수동으로 `deletedAt IS NULL` 조건을 추가해야 한다.

**개선안**: Hibernate `@Where` 또는 `@SQLRestriction`(Hibernate 6.3+) 적용.

```java
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseEntity { ... }
```

### 9. 배치에서 직접 SQL 사용 - 도메인 로직 우회

**현재**: `CouponExpireTasklet`이 `JdbcTemplate`으로 직접 UPDATE SQL을 실행한다. 도메인 엔티티의 상태 변경 로직(`Coupon` 메서드)을 우회한다.

**트레이드오프**: 대량 데이터 처리 시 벌크 SQL이 성능상 유리하므로, 의도적인 설계 결정이라면 주석으로 사유를 남기는 것을 권장한다. 데이터 규모가 크지 않다면 Chunk 기반 처리로 도메인 로직을 활용하는 것도 고려할 수 있다.

### 10. 테스트 부족

**현재**: 통합 테스트 1개(`AuthControllerIntegrationTest`), 컨텍스트 로드 테스트 1개만 존재한다.

**개선안**:

| 테스트 유형 | 대상 |
|------------|------|
| 단위 테스트 | `Coupon.use()`, `Coupon.register()`, `Coupon.isExpiredAt()` |
| 단위 테스트 | `JwtTokenProvider` 토큰 생성/검증 |
| 서비스 테스트 | `CouponService`, `RegisterCouponService` |
| API 테스트 | `CouponController` 각 엔드포인트 |
| 배치 테스트 | `CouponExpireTasklet` 만료 처리 |

### 11. JWT Secret 하드코딩

**현재**: `JwtProperties`에 기본값으로 시크릿 키가 하드코딩되어 있고, `application.yaml`에도 동일 값이 그대로 노출되어 있다.

**개선안**: 프로파일별 설정 분리. 운영 환경에서는 환경 변수나 Vault 등으로 주입.

```yaml
# application.yaml (공통)
security:
  jwt:
    access-token-expiration-seconds: 3600

# application-local.yaml
security:
  jwt:
    secret: local-dev-secret-key-at-least-32-bytes

# 운영: 환경변수 SECURITY_JWT_SECRET 사용
```

### 12. Lombok 어노테이션 프로세서 누락

**현재**: root `build.gradle`에서 `compileOnly 'org.projectlombok:lombok'`만 선언하고 `annotationProcessor`가 없다. 빌드 환경에 따라 Lombok이 정상 동작하지 않을 수 있다.

**개선안**:

```gradle
subprojects {
    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
    }
}
```

---

## 우선순위 정리

| 우선순위 | 항목 | 사유 |
|---------|------|------|
| **높음** | AuthController 위치 이동 | 모듈 책임 원칙 위반 |
| **높음** | 서비스 중복 제거 | 유지보수 혼란 |
| **높음** | 예외 처리 전략 | API 안정성 |
| **높음** | Lombok annotationProcessor 추가 | 빌드 안정성 |
| **중간** | User 도메인 패턴 통일 | 일관성 |
| **중간** | CouponRepository 메서드 보완 | 아키텍처 일관성 |
| **중간** | API 응답 표준화 | API 품질 |
| **중간** | Soft Delete 필터링 | 데이터 정합성 |
| **낮음** | CouponIssuance 정리 | 코드 정리 |
| **낮음** | 배치 도메인 로직 활용 | 트레이드오프 존재 |
| **지속** | 테스트 코드 보강 | 품질 보증 |
| **지속** | JWT Secret 환경분리 | 보안 |
