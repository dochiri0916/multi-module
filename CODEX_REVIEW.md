# CODEX Review

작성일: 2026-03-07  
대상 프로젝트: `multi-module`

참고: 이 문서는 `common` 모듈이 `error-handling` / `time`으로 분리되기 전 상태를 기준으로 작성되었습니다.

## 1. 분석 범위와 검증

- 분석 범위: 루트 빌드 설정, `modules/common`, `modules/jpa`, `modules/security`, `README.md`, `COMMON_MODULE_DESIGN.md`
- 정적 확인: main Java 27개, test Java 15개, 추적 파일 58개
- 실행 검증:
  - `./gradlew test` 실행
  - 결과: `BUILD SUCCESSFUL`
  - 테스트 결과: 56 passed, 0 failed, 0 skipped
- 배포 관점 검증:
  - `./gradlew :modules:security:dependencies --configuration runtimeClasspath`
  - `./gradlew :modules:common:generatePomFileForMavenJavaPublication :modules:jpa:generatePomFileForMavenJavaPublication :modules:security:generatePomFileForMavenJavaPublication`

## 2. 총평

- 모듈 경계는 비교적 명확하고, Auto Configuration 기반 라이브러리 구조도 깔끔합니다.
- 단위 테스트 품질은 좋은 편이며 JWT, 예외 응답, 프로퍼티 기본값은 안정적으로 검증되어 있습니다.
- 다만 현재 상태는 문서가 약속하는 "필요한 모듈만 추가해서 바로 사용"과 실제 배포 산출물이 완전히 일치하지 않습니다.
- 가장 큰 리스크는 "독립 모듈" 계약과 "배포 POM/런타임 클래스패스"가 어긋난 지점입니다. 이 부분은 실제 소비자 프로젝트에서 시작 실패나 컴파일 실패로 바로 드러날 수 있습니다.

## 3. 주요 발견사항

| 우선순위 | 항목 | 영향 |
|---|---|---|
| High | `security` 모듈이 실제로는 독립 실행 가능하지 않음 | 소비자 앱 시작 실패 가능 |
| High | `jpa` 단독 사용 시 `createdBy` 감사값 계약이 깨짐 | 저장 시 제약조건 위반 가능 |
| Medium | 공개 API와 배포 의존성 scope가 맞지 않음 | 소비자 컴파일 계약이 불안정함 |
| Medium | Refresh 토큰 검증 예외 계약이 README와 다름 | 호출부 예외 처리 정책 혼선 |
| Medium | 공통 `Clock`이 모듈 내부에서 사실상 활용되지 않음 | 시간대 정책 일관성/테스트성 저하 |
| Medium | JWT 발급 입력 계약이 느슨함 | 잘못된 권한 문자열 또는 NPE 가능 |
| Low | 설계 문서가 현재 코드와 많이 어긋남 | 온보딩/유지보수 혼선 |

### 3.1 High: `security` 모듈이 실제로는 독립 실행 가능하지 않음

- 근거
  - `spring-data-commons`가 `compileOnly`로 선언되어 있음: `modules/security/build.gradle:15`
  - 그런데 `SecurityAutoConfiguration`은 `SecurityAuditAutoConfiguration`을 항상 import함: `modules/security/src/main/java/com/dochiri/security/configuration/SecurityAutoConfiguration.java:7-12`
  - `SecurityAuditAutoConfiguration`은 런타임에 `AuditorAware`를 직접 참조함: `modules/security/src/main/java/com/dochiri/security/configuration/SecurityAuditAutoConfiguration.java:8-16`
  - 생성된 배포 POM에는 `spring-data-commons`가 없음: `modules/security/build/publications/mavenJava/pom-default.xml:12-67`
  - README는 필요한 모듈만 선택 추가 가능하다고 안내함: `README.md:59-64`
- 문제
  - 현재 배포 산출물 기준으로 `dochiri-security`만 추가한 소비자 앱은 `org.springframework.data.domain.AuditorAware` 부재로 자동설정 로딩 단계에서 실패할 가능성이 큽니다.
  - 즉, 문서상 독립 모듈이지만 실제로는 Spring Data 타입에 암묵적으로 묶여 있습니다.
- 권장 조치
  - 가장 안전한 방향은 `SecurityAuditAutoConfiguration`을 `@ConditionalOnClass(AuditorAware.class)`로 분리하거나 별도 integration 모듈로 떼는 것입니다.
  - 만약 감사 기능을 `security` 모듈의 필수 구성으로 볼 것이라면 `spring-data-commons`를 `implementation`이 아니라 실제 런타임에 필요한 형태로 노출해야 합니다.
  - 소비자 관점 smoke test를 추가해 `dochiri-security` 단독 의존 시 컨텍스트 기동 여부를 검증하는 것이 좋습니다.

### 3.2 High: `jpa` 단독 사용 시 `createdBy` 감사값 계약이 깨짐

- 근거
  - `BaseEntity.createdBy`는 `nullable = false`로 강제됨: `modules/jpa/src/main/java/com/dochiri/jpa/entity/BaseEntity.java:31-33`
  - `JpaAutoConfiguration`은 감사 기능만 켜고 기본 `AuditorAware`는 제공하지 않음: `modules/jpa/src/main/java/com/dochiri/jpa/configuration/JpaAutoConfiguration.java:6-8`
  - `jpa` 모듈은 `security`를 의존하지 않음: `modules/jpa/build.gradle:7-10`
  - 기본 `AuditorAware` 제공은 `security` 쪽에만 있음: `modules/security/src/main/java/com/dochiri/security/configuration/SecurityAuditAutoConfiguration.java:13-16`
  - README는 모듈을 선택적으로 추가할 수 있다고 설명함: `README.md:59-64`
- 문제
  - `jpa`만 사용하는 소비자 앱에서 `BaseEntity`를 상속한 엔티티를 저장하면 `createdBy`가 채워지지 않아 DB 제약조건 위반으로 이어질 수 있습니다.
  - 이 이슈는 테스트에서 드러나지 않았는데, 현재 테스트는 `BaseEntity`의 필드/soft delete만 확인하고 실제 JPA Auditing 계약은 검증하지 않기 때문입니다.
- 권장 조치
  - 선택지 1: `jpa` 모듈에 기본 `AuditorAware<Long>`를 제공
  - 선택지 2: `AuditorAware` 빈이 없으면 컨텍스트를 fail-fast로 종료
  - 선택지 3: `jpa` 단독 사용을 공식 비지원으로 문서화하고 조합 모듈 사용을 강제

### 3.3 Medium: 공개 API와 배포 의존성 scope가 맞지 않음

- 근거
  - `common`은 `spring-web`, `spring-webmvc`를 `implementation`으로 선언함: `modules/common/build.gradle:5-7`
  - 하지만 공개 API인 `ErrorCode`는 `HttpStatus`를 반환하고: `modules/common/src/main/java/com/dochiri/common/exception/ErrorCode.java:3-11`
  - 공개 API인 `GlobalExceptionHandler`는 `ResponseEntityExceptionHandler`를 상속함: `modules/common/src/main/java/com/dochiri/common/exception/GlobalExceptionHandler.java:6-15`
  - 생성된 `dochiri-common` POM은 이 둘을 `runtime` scope로 내보냄: `modules/common/build/publications/mavenJava/pom-default.xml:19-35`
  - 같은 패턴이 `security`에도 있음. `JwtProvider`는 공개 메서드에서 `io.jsonwebtoken.Claims`를 사용하지만: `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:46-55`
  - 생성된 `dochiri-security` POM은 `jjwt-api`를 `runtime` scope로 내보냄: `modules/security/build/publications/mavenJava/pom-default.xml:49-53`
  - README는 `ErrorCode` 구현과 `GlobalExceptionHandler` 상속을 직접 안내함: `README.md:78-90`, `README.md:120-129`
- 문제
  - 현재 산출물은 "공개 API가 요구하는 타입"과 "소비자에게 제공되는 compile classpath"가 어긋나 있습니다.
  - Spring Boot starter를 넓게 쓰는 앱에서는 우연히 가려질 수 있지만, Maven 소비자나 최소 의존성 구성에서는 문서 예제를 따라도 컴파일 문제가 날 수 있습니다.
- 권장 조치
  - 공개 시그니처에 노출되는 라이브러리는 `api`로 승격하는 편이 맞습니다.
  - 최소한 `common`의 `spring-web`/`spring-webmvc`, `security`의 `jjwt-api`는 배포 POM 기준 다시 검토해야 합니다.
  - `publishToMavenLocal` 후 샘플 소비자 프로젝트를 실제로 컴파일하는 검증 단계를 CI에 넣는 것을 권장합니다.

### 3.4 Medium: Refresh 토큰 검증 예외 계약이 README와 다름

- 근거
  - README는 "유효하지 않은 리프레시 토큰이면 `BadCredentialsException` 발생"이라고 설명함: `README.md:214-216`
  - 실제 구현은 토큰 종류가 refresh가 아닐 때만 `BadCredentialsException`을 던짐: `modules/security/src/main/java/com/dochiri/security/jwt/RefreshTokenVerifier.java:14-21`
  - 만료/서명 오류 등은 `jwtProvider.parseAndValidate` 예외가 그대로 전파됨: `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:46-51`
  - 테스트도 만료 토큰에 대해 구체 예외가 아니라 `Exception`만 검증함: `modules/security/src/test/java/com/dochiri/security/jwt/RefreshTokenVerifierTest.java:42-48`
- 문제
  - 호출부는 문서만 보면 `BadCredentialsException` 하나만 처리하면 된다고 생각하기 쉽지만, 실제로는 JJWT 예외까지 알아야 합니다.
  - 이 불일치는 인증 재발급 API의 예외 처리 정책을 모호하게 만듭니다.
- 권장 조치
  - `verifyAndExtractUserId`에서 JWT 파싱/subject 변환 예외를 `BadCredentialsException`으로 래핑해 계약을 단일화하는 편이 좋습니다.
  - 반대로 여러 예외를 그대로 노출할 의도라면 README와 테스트를 그 방향으로 바꿔야 합니다.

### 3.5 Medium: 공통 `Clock`이 모듈 내부에서 사실상 활용되지 않음

- 근거
  - `common`은 `Clock` 빈을 등록함: `modules/common/src/main/java/com/dochiri/common/configuration/CommonAutoConfiguration.java:10-17`
  - README도 `common.timezone`을 중요한 설정으로 소개함: `README.md:152-170`
  - 하지만 `JwtProvider`는 `LocalDateTime.now()`와 `System.currentTimeMillis()`를 직접 사용함: `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:42-43`, `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:75-83`
  - `JpaAutoConfiguration`에는 `Clock` 또는 `DateTimeProvider` 연동이 없음: `modules/jpa/src/main/java/com/dochiri/jpa/configuration/JpaAutoConfiguration.java:6-8`
  - `Clock` 빈도 `@ConditionalOnMissingBean` 없이 등록됨: `modules/common/src/main/java/com/dochiri/common/configuration/CommonAutoConfiguration.java:14-16`
- 문제
  - 현재 구조에서는 공통 시간대 설정이 라이브러리 내부 timestamp 생성에 거의 반영되지 않습니다.
  - 특히 `refreshTokenExpiresAt`은 JVM 기본 시간대에 좌우되고, 소비자 앱이 자체 `Clock`을 제공하려고 해도 충돌 가능성이 있습니다.
- 비고
  - JPA auditing 시간값이 기본 구현에 의존한다는 부분은 `DateTimeProvider` 부재를 근거로 한 추론입니다.
- 권장 조치
  - `Clock`을 `JwtProvider`에 주입하고, JPA auditing도 `DateTimeProvider` 또는 별도 설정으로 같은 시간 소스를 사용하게 맞추는 것이 좋습니다.
  - 동시에 `Clock` 빈은 `@ConditionalOnMissingBean(Clock.class)`로 완화하는 편이 안전합니다.

### 3.6 Medium: JWT 발급 입력 계약이 느슨함

- 근거
  - 토큰 생성 시 `userId`, `role`에 대한 사전 검증이 없음: `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:34-40`, `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:75-83`
  - 구현은 `userId.toString()`과 `"ROLE_" + role`에 의존함: `modules/security/src/main/java/com/dochiri/security/jwt/JwtProvider.java:79`, `modules/security/src/main/java/com/dochiri/security/jwt/JwtAuthenticationConverter.java:30-34`
- 문제
  - `userId == null`이면 즉시 NPE가 발생합니다.
  - `role`에 공백, 소문자, 이미 `ROLE_`이 붙은 값이 들어오면 권한 문자열이 비정상적으로 만들어질 수 있습니다.
  - 현재 테스트는 정상 입력만 검증하고 있어 이 계약이 암묵적으로 남아 있습니다.
- 권장 조치
  - `requireNonNull(userId)`와 `role.isBlank()` 검증을 추가하고, 허용 role 포맷을 코드 또는 문서로 명시하는 것이 좋습니다.

### 3.7 Low: 설계 문서가 현재 코드와 많이 어긋남

- 근거
  - `COMMON_MODULE_DESIGN.md`는 루트 구조, 클래스명, Boot/Java 버전을 오래된 상태로 설명함: `COMMON_MODULE_DESIGN.md:32-41`, `COMMON_MODULE_DESIGN.md:64-73`, `COMMON_MODULE_DESIGN.md:136-145`, `COMMON_MODULE_DESIGN.md:165-170`
  - 실제 프로젝트는 `common` 모듈이 추가되어 있고: `settings.gradle:3-6`
  - Boot `4.0.3`, Java `25`를 사용함: `build.gradle:19-28`
- 문제
  - 신규 참여자가 설계 문서를 먼저 보면 실제 코드 구조와 다르게 이해할 가능성이 큽니다.
- 권장 조치
  - 이 문서는 현재 구조에 맞게 전면 동기화하거나, 더 이상 기준 문서가 아니라면 deprecated 표시가 필요합니다.

## 4. 강점

- 모듈 책임 분리는 명확합니다. `common`, `jpa`, `security`가 섞여 있지 않습니다.
- Auto Configuration 등록 방식이 표준적입니다. `AutoConfiguration.imports` 구성도 깔끔합니다.
- `BaseException`과 Problem Detail 응답 설계는 재사용 라이브러리로서 방향이 좋습니다.
- `SecurityFilterChain`은 기본 구현과 소비자 오버라이드 지점을 함께 제공해 확장성이 괜찮습니다.
- 테스트는 happy path와 핵심 예외 path를 최소 수준 이상 커버하고 있습니다.

## 5. 테스트 관점 코멘트

- 현재 테스트는 단위 테스트 중심입니다. 내부 로직 회귀를 막는 데는 충분히 도움이 됩니다.
- 반면 아래 소비자 관점 테스트가 빠져 있어서 이번 핵심 이슈들이 숨겨졌습니다.
  - `dochiri-security` 단독 의존 컨텍스트 기동 테스트
  - `dochiri-jpa` 단독 사용 시 Auditing 계약 테스트
  - `publishToMavenLocal` 이후 샘플 소비자 프로젝트 컴파일/기동 테스트
  - `Clock` 커스터마이징 및 시간대 일관성 테스트

## 6. 권장 우선순위

1. `security`의 `spring-data-commons` 런타임 의존 문제부터 정리
2. `jpa` 단독 사용 계약을 확정하고 `AuditorAware` 전략을 명시
3. 공개 API에 노출되는 의존성을 `api` 기준으로 재분류
4. Refresh 토큰 예외 계약을 코드/문서/테스트에서 하나로 통일
5. `Clock`을 공통 시간 소스로 실제 사용하도록 연결
6. `README.md`, `COMMON_MODULE_DESIGN.md`를 현재 계약에 맞게 업데이트

## 7. 결론

- 내부 코드 품질은 전반적으로 나쁘지 않지만, 라이브러리로 배포했을 때의 소비자 계약은 아직 완성도가 부족합니다.
- 특히 "모듈 독립성"과 "배포 의존성 scope"는 지금 바로 정리하지 않으면 이후 실제 서비스 통합 단계에서 시간을 크게 잃을 가능성이 높습니다.
- 우선순위는 기능 추가보다 배포 계약 정합성 확보에 두는 편이 맞습니다.
