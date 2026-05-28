# JPA Module

`modules:jpa`는 JPA 기반 서비스에서 공통으로 사용할 `BaseEntity`, JPA auditing, Querydsl `JPAQueryFactory` 자동 등록을 제공하는 모듈이다.

## 현재 구현

- `BaseEntity`
  - `createdAt`, `updatedAt`
  - `createdBy`, `updatedBy`
  - `@MappedSuperclass`
  - `AuditingEntityListener` 사용
- `JpaAutoConfiguration`
  - `@EnableJpaAuditing`
  - `AuditorAware<String>` fallback bean 등록
  - 기본 auditor는 `security.system-user-id` 또는 `dochiri.jpa.audit.system-user-id`
- `QueryDslAutoConfiguration`
  - `EntityManager`와 `JPAQueryFactory`가 classpath에 있으면 `JPAQueryFactory` bean 등록
- auto-configuration imports
  - `JpaAutoConfiguration`
  - `QueryDslAutoConfiguration`

## 문제점

### 1. auditor 타입 정책이 흔들렸음

`BaseEntity.createdBy`, `updatedBy`는 `String`인데, security 모듈은 기존에 `AuditorAware<Long>`를 제공했다. 이 차이 때문에 auditing 통합에서 타입 불일치나 기대값 혼란이 생길 수 있었다.

1단계에서 확정한 정책:

- `BaseEntity.createdBy`, `updatedBy`는 `String`으로 유지한다.
- security 모듈의 auditor도 `AuditorAware<String>`로 맞춘다.
- 사용자 id가 `Long`이어도 auditing 저장값은 문자열 표현으로 저장한다.

### 2. `@EnableJpaAuditing` 자동 적용의 영향 범위가 큼

JPA 모듈을 의존하는 순간 auditing이 자동 활성화된다. 대부분의 API 서비스에는 편하지만, 일부 batch/worker/test context에서는 원하지 않는 동작일 수 있다.

### 3. fallback auditor 정책이 환경 직접 바인딩에 의존함

`JpaAuditProperties.from(Environment)`가 직접 Binder를 사용한다. 동작은 단순하지만 configuration properties로 드러나지 않아 문서화와 metadata 생성이 약하다.

### 4. Querydsl 의존성이 모든 JPA 소비자에게 전파됨

현재 `querydsl-jpa`가 `api`로 노출된다. Querydsl을 쓰지 않는 서비스도 의존성을 받는다.

### 5. 현재 JPA 통합 테스트 실패가 있음

전체 테스트에서 `BaseEntityIntegrationTest`의 auditing assertion이 실패하고 있다. 리팩토링 전 이 실패 원인을 먼저 고정해야 한다.

## 리팩토링 방향

### 1단계: auditing 타입 정책 확정

상태: 완료

- `BaseEntity.createdBy`, `updatedBy`를 `String`으로 유지한다.
- security 모듈의 auditor도 `AuditorAware<String>`로 맞춘다.
- 사용자 id가 `Long`이어도 auditing 저장값은 문자열 표현으로 저장한다.

이유:

- 공통 엔티티가 특정 사용자 id 타입에 묶이지 않는다.
- 시스템 사용자, 외부 인증 subject, service account 같은 값을 표현하기 쉽다.
- 기존 `length = 36` 정책과도 맞다.

대안:

- `BaseEntity`를 `Long` auditor 전용으로 바꾸는 방식
- `StringAuditableEntity`, `LongAuditableEntity`를 분리하는 방식

### 2단계: `JpaAuditProperties`를 정식 properties로 전환

`@ConfigurationProperties(prefix = "dochiri.jpa.audit")` 형태로 명시한다.

검토할 property:

```yaml
dochiri:
  jpa:
    audit:
      enabled: true
      system-user-id: "0"
```

하위 호환:

- 기존 `security.system-user-id`를 한동안 fallback으로 읽는다.
- 문서에서는 `dochiri.jpa.audit.system-user-id`를 우선 경로로 안내한다.

### 3단계: auditing auto-configuration 조건 강화

검토할 조건:

- `@ConditionalOnClass(EntityManager.class)`
- `@ConditionalOnProperty(prefix = "dochiri.jpa.audit", name = "enabled", havingValue = "true", matchIfMissing = true)`
- `@ConditionalOnMissingBean(AuditorAware.class)`

목표:

- 기본은 편하게 동작한다.
- 필요하면 소비 프로젝트가 auditing을 끄거나 자체 auditor를 등록할 수 있다.

### 4단계: Querydsl 자동 등록 정책 정리

선택지:

- 현재처럼 `querydsl-jpa`를 `api`로 유지한다.
- `implementation`으로 낮추고 Querydsl 사용 서비스가 직접 의존하게 한다.
- Querydsl 자동 등록을 별도 모듈로 분리한다.

권장:

- 지금은 `jpa` 모듈에 유지하되 문서에 "Q 클래스 annotation processor는 소비 프로젝트가 직접 추가"해야 한다고 명시한다.
- Querydsl을 쓰지 않는 서비스에서 의존성 부담이 커지면 별도 모듈 분리를 검토한다.

### 5단계: 테스트 안정화

우선순위:

1. 현재 실패 중인 `BaseEntityIntegrationTest` 원인 확인
2. auditor 타입 정책 반영
3. fallback auditor 테스트
4. custom `AuditorAware` 우선순위 테스트
5. Querydsl bean 등록 테스트

## 사용 예시

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.dochiri:dochiri-jpa:0.0.1-SNAPSHOT'
}
```

```java
@Entity
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
}
```

## 완료 기준

- auditor 타입 정책이 명확하다. 완료
- security 모듈과 auditing 타입이 충돌하지 않는다. 완료
- 소비 프로젝트가 자체 `AuditorAware`를 등록할 수 있다.
- Querydsl 사용 조건과 annotation processor 설정이 문서화되어 있다.
- `./gradlew :modules:jpa:test`가 통과한다.
