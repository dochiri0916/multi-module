# JPA Modules

JPA 공통 기능은 auditing과 QueryDSL의 선택 단위를 분리한다. `modules:jpa`는 두 기능을 함께 쓰는 기존 소비자를 위한 aggregator다.

## 모듈

| 모듈 | 책임 | 주요 전이 의존성 |
| --- | --- | --- |
| `jpa-auditing` | `BaseEntity`, JPA auditing, fallback `AuditorAware<String>` | Spring Data JPA |
| `jpa-querydsl` | `JPAQueryFactory` 자동 구성 | Spring Data JPA, QueryDSL JPA |
| `jpa` | 위 두 모듈 aggregator | 두 모듈 전체 |

QueryDSL이 필요 없다면 `dochiri-jpa-auditing`만 의존한다.

## JPA auditing

`BaseEntity`는 다음 기술 감사 필드를 제공하는 `@MappedSuperclass`다.

- `createdAt`, `updatedAt`: `Instant`
- `createdBy`, `updatedBy`: `String`

감사자 문자열은 DB 기술 키 타입에 묶이지 않으며 인증 subject, service account, system actor를 표현할 수 있다.

```yaml
dochiri:
  jpa:
    audit:
      system-subject: system
```

`JpaAuditProperties`는 `@ConfigurationProperties(prefix = "dochiri.jpa.audit")` record다. blank 또는 누락된 system subject는 `system`으로 정규화한다.

`JpaAuditingAutoConfiguration`은 필요한 Spring Data class가 있을 때 auditing을 활성화한다. 소비자가 `AuditorAware` Bean을 제공하면 fallback 구현은 물러난다.

```java
@Bean
AuditorAware<String> serviceAuditor() {
    return () -> Optional.of("order-service");
}
```

`security-jpa`를 함께 사용하면 `SecurityAuditAutoConfiguration`이 먼저 Security Context 기반 auditor를 제공한다.

## QueryDSL

`jpa-querydsl`은 `EntityManager`가 있고 사용자 `JPAQueryFactory`가 없을 때 기본 factory를 등록한다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-jpa-querydsl:1.0.0'
}
```

소비 프로젝트에서 Q class 생성이 필요하면 해당 프로젝트의 Entity source set에 QueryDSL annotation processor를 설정한다. 모듈의 factory 자동 구성은 소비 프로젝트의 Q class 생성 책임을 대신하지 않는다.

사용자 factory를 제공할 수 있다.

```java
@Bean
JPAQueryFactory applicationQueryFactory(EntityManager entityManager) {
    return new JPAQueryFactory(entityManager);
}
```

이 경우 기본 Bean은 등록되지 않는다.

## 선택 예시

auditing만 사용:

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-jpa-auditing:1.0.0'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

auditing과 QueryDSL을 함께 사용:

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-jpa:1.0.0'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

## Entity 사용 원칙

- 소비 Context의 JPA Entity는 Adapter 계층에 둔다.
- Aggregate 간 참조는 대상 식별자 값으로 저장한다.
- JPA 객체 연관관계와 relation annotation을 사용하지 않는다.
- DB 기술 키 `Long id`는 Entity 내부에만 두고 외부에 노출하지 않는다.
- 비즈니스 규칙은 Domain Aggregate/VO가 소유하며 `BaseEntity`에는 넣지 않는다.
- 운영 schema는 versioned migration이 소유한다.

## 자동 구성

```text
jpa-auditing:
  com.dochiri.jpa.adapter.in.bootstrap.JpaAuditingAutoConfiguration

jpa-querydsl:
  com.dochiri.jpa.adapter.in.bootstrap.QueryDslAutoConfiguration
```

두 자동 구성 모두 소비자 Bean back-off를 테스트한다.

## 검증

```bash
./gradlew :modules:jpa-auditing:test
./gradlew :modules:jpa-querydsl:test
./gradlew :modules:jpa:test
./gradlew check
```

테스트는 fallback/custom auditor, auditing 통합 저장, QueryDSL classpath 선택성, 기본 factory 및 사용자 factory back-off를 검증한다.
