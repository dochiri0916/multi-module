# Multi-Module Project Review

## 종합 평가: 7.5 / 10

아키텍처는 견고하고 Auto-configuration 패턴이 올바르게 적용되어 있으며, Security/JWT 구현이 잘 설계되어 있다. 주요 개선점은 의존성 설정과 문서 일관성에 있다.

---

## 프로젝트 구조

```
multi-module/
├── settings.gradle
├── build.gradle
├── modules/
│   ├── common/     # 공통 예외 처리, Clock 설정
│   ├── jpa/        # JPA Auditing, QueryDSL, BaseEntity
│   └── security/   # JWT 인증, Security FilterChain, CORS, Auditor
```

### 모듈 의존성 그래프

```
common  (spring-context)
  ↑
  ├── jpa      (+ spring-boot-starter-data-jpa, QueryDSL)
  └── security (+ spring-boot-starter-security, JJWT)
```

- 순환 의존성: 없음
- 모듈 간 책임 분리: 명확

---

## 잘된 점

### 1. 라이브러리 모듈 구조
- `java-library` 플러그인만 사용하여 표준 JAR로 빌드 (fat JAR 아님)
- `spring-boot` 플러그인 미적용 — 라이브러리 프로젝트에 적합

### 2. Auto-configuration 패턴
- `@AutoConfiguration` + `AutoConfiguration.imports` 올바르게 구성
- `@ConditionalOnMissingBean`으로 소비자 프로젝트에서 오버라이드 가능
- `proxyBeanMethods = false`로 성능 최적화
- `@EnableConfigurationProperties`로 프로퍼티 바인딩

### 3. Security 모듈 설계
- `SecurityAutoConfiguration`이 `@Import`로 하위 설정을 조합하는 구조
- JWT, CORS, FilterChain, Audit를 독립 Configuration으로 분리
- `@ConditionalOnMissingBean(SecurityFilterChain.class)` — 소비자가 커스텀 체인 제공 가능
- RFC 9457 Problem Detail 형식의 에러 응답

### 4. 코드 품질
- `record` 활용 (JwtPrincipal, JwtTokenResult, Properties 클래스)
- Properties에 `@Validated` + Bean Validation 적용
- BaseEntity의 Soft Delete 패턴 (`@SQLRestriction`)
- 일관된 패키지 네이밍 (`configuration`, `exception`, `entity` 등)

### 5. Gradle 설정
- `api` vs `implementation` 적절히 구분
- Spring Boot BOM으로 버전 관리 통일
- `maven-publish` + `versionMapping`으로 올바른 POM 생성

---

## 남은 개선 사항

### Critical

#### 3. 문서 버전 불일치

**파일:** `COMMON_MODULE_DESIGN.md`

설계 문서에 이전 버전(Spring Boot 3.x, Java 21 등) 정보가 남아있으나, 실제 프로젝트는 Spring Boot 4.0.3 + Java 25를 사용한다.

---

### Low

#### 9. Auto-configuration 순서 미지정

`@AutoConfigureBefore` / `@AutoConfigureAfter`가 사용되지 않는다. 현재 순서 의존성이 없어 문제되지 않지만, 이 가정을 주석이나 문서로 명시하는 것이 좋다.

---

## 모듈별 상세

### common

| 항목 | 평가 |
|------|------|
| BaseException | RFC 9457 준수, null-safe, 잘 설계됨 |
| ErrorCode | 인터페이스 기반 — 소비자가 enum으로 구현 가능 |
| GlobalExceptionHandler | 5xx/4xx 로그 레벨 분리, 적절한 에러 응답 |
| CommonAutoConfiguration | 단순하고 명확, 단일 책임 |

### jpa

| 항목 | 평가 |
|------|------|
| BaseEntity | Soft delete, JPA Auditing, 잘 설계됨 |
| JpaAutoConfiguration | `@EnableJpaAuditing` 활성화 — 적절 |
| QueryDslAutoConfiguration | JPAQueryFactory 빈 제공 — 편의성 좋음 |
| 의존성 | `api`로 QueryDSL + JPA 전이 — 올바름 |

### security

| 항목 | 평가 |
|------|------|
| JWT 구현 | HMAC-SHA256, access/refresh 분리, 잘 설계됨 |
| FilterChain | ConditionalOnMissingBean, stateless, CORS 지원 |
| Properties | record + @Validated, 최소 비밀키 길이 검증 |
| SecurityResponseWriter | Problem Detail 형식, 유틸리티 패턴 |
| SecurityAuditorAware | Spring Data Auditing 연동, 적절한 fallback |

---

## 요약

| 우선순위 | 항목 | 난이도 |
|----------|------|--------|
| Critical | COMMON_MODULE_DESIGN.md 버전 정보 업데이트 | 낮음 |
| Low | Auto-configuration 순서 가정 문서화 | 낮음 |
| Low | ~~테스트 코드 추가~~ (완료, 38개) | - |
