# error-handling 아키텍처 리뷰

## 1. 리뷰 범위

- 대상: `modules:error-handling-starter`
- 기준일: 2026-07-17
- 기준: Java 21, Spring Boot 4, RFC 9457, Clean Architecture, Hexagonal Architecture, DDD 경계, 객체 지향, 소비 프로젝트 사용성
- 소비 검증 프로젝트: `/Users/seongbin/programming/laboratory/tool-test-lab`

기존 `modules:error-handling` engine은 Gradle 설정뿐 아니라 소스, 빈 package 경로, 과거 class/JAR/test report를 포함한 물리 디렉터리까지 제거했다. 이 기능은 Domain/Application 모듈이 아니라 공통 Web Adapter이므로 Aggregate, JPA, Repository, 트랜잭션 규칙은 적용 대상이 아니다.

## 2. 최종 판정

**승인(Pass)**

공통 오류 처리에 필요한 Spring 공식 확장점만 남겼으며, 비즈니스 오류의 HTTP 의미는 각 Bounded Context의 Web Adapter가 소유한다.

| 평가 영역 | 판정 | 근거 |
| --- | --- | --- |
| Clean Architecture | 충족 | Domain/Application이 Spring Web과 starter를 참조하지 않는다. |
| Hexagonal Architecture | 충족 | HTTP 변환이 Context의 Inbound Web Adapter에 위치한다. |
| DDD 경계 | 충족 | 공통 handler가 Context 예외를 import하지 않는다. |
| 객체 지향/SRP | 충족 | 자동 구성과 예외 처리라는 두 변경 이유를 두 클래스로 분리했다. |
| 공식 표준 | 충족 | RFC 9457 `ProblemDetail`과 Spring 공식 예외 처리 API를 사용한다. |
| 소비 사용성 | 충족 | 기술 예외는 starter 하나로 처리하고 비즈니스 예외만 Context Advice에 명시한다. |

## 3. Production 코드 규모

리팩터링 시작 시 `error-handling` engine에는 tracked 17개와 당시 작업 중이던 untracked 8개를 합쳐 Production Java 파일이 25개 있었다. 리팩터링 후 공통 starter에는 두 개만 남았다.

```text
modules/error-handling-starter/src/main/java/
└── com/dochiri/errorhandling/global/error/
    ├── ErrorHandlingAutoConfiguration.java
    └── GlobalExceptionHandler.java
```

23개가 줄어 약 92% 감소했다. 별도 저수준 engine artifact도 제거했다.

리팩터링 과정에서 남았던 다른 빈 source package 경로와 `security-error-webmvc`가 Production에서 사용하지 않던 `compileOnly servlet-api`도 함께 제거했다. Servlet 기반 auto-configuration 테스트가 실제로 사용하는 test 의존성은 유지했다.

## 4. 사용자 합의에 따른 컨벤션 재정의

저장소 기본 컨벤션에는 `ApiExceptionMapper`, API error code, message catalog를 사용하는 확장형 오류 구조가 포함돼 있었다. 이번 요청에서는 그 구조보다 RFC 9457과 Spring 공식 확장점만 남기는 최소 구성을 명시적으로 선택했다.

따라서 다음 두 결정은 누락이 아니라 이번 모듈에 한정한 합의된 재정의다.

- 비즈니스 title/detail은 공통 catalog가 아니라 해당 Context Web Adapter가 소유한다.
- 공통 handler에는 정보 노출을 막는 고정 500 title/detail 두 개만 둔다.

RFC 9457은 message catalog나 별도 API code를 요구하지 않는다. 국제화나 중앙 문구 관리가 실제 제품 요구사항이 되면 그때 별도 책임으로 추가한다.

## 5. 제거한 자체 프레임워크

다음 개념은 Spring과 RFC가 요구하지 않고 현재 요구사항에도 필요하지 않아 제거했다.

- `ApiErrorContributor`, `ApiErrorDefinition`
- `ApiErrorCode`, `ApiErrorMapping`, `ApiErrorMessage`
- mapping/message provider
- exception mapper와 message catalog
- contract validator
- package 기반 API code 생성
- custom validation field 오류 모델
- 자체 trace ID filter

하나의 오류 계약을 mapping과 message로 분리한 뒤 다시 조립하던 흐름도 사라졌다.

```text
이전
Exception -> Contributor/Provider -> Mapper/Catalog -> Factory -> Handler

현재
Exception -> Context @ExceptionHandler -> ProblemDetail
```

## 6. 계층 경계

```text
consumer.domain <- consumer.application <- consumer.adapter.in.web
                                             |
                                             v
                                      Spring ProblemDetail
```

- Domain/Application 예외는 의미 error code와 필요한 상태만 보관한다.
- HTTP status, type, title, detail은 Context Web Adapter가 결정한다.
- `GlobalExceptionHandler`는 특정 Context 예외를 import하거나 분기하지 않는다.
- 미처리 예외는 원본을 서버 로그에 남기고 내부 문자열을 숨긴 500으로 반환한다.
- 소비 애플리케이션에 기존 `ResponseEntityExceptionHandler` Bean이 있으면 공통 handler가 자동으로 물러나 중복 Advice를 만들지 않는다.

Domain 예외에 `@ResponseStatus`를 붙이거나 `ErrorResponse`를 구현하는 방식은 더 짧지만 안쪽 계층을 Spring Web에 결합하므로 사용하지 않았다.

## 7. Spring과 RFC 사용

starter는 다음 공식 API만 사용한다.

- `ProblemDetail`
- `ResponseEntityExceptionHandler`
- `@RestControllerAdvice`
- `@ExceptionHandler`
- Spring Boot Auto-configuration

기본 응답은 RFC 9457의 `type`, `title`, `status`, `detail`, `instance`만 사용한다. `code`, `traceId`, `fieldErrors`는 제품 요구사항이 생기기 전까지 추가하지 않는다.

일반 500은 RFC 기본 problem type인 `about:blank`를 사용한다. Spring은 기본값인 `type` 필드를 JSON에서 생략할 수 있으며 이는 표준 위반이 아니다.

## 8. 비즈니스 예외 확장

소비 Context는 Web Adapter에 Advice 하나를 둔다.

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class ProductExceptionHandler {

    @ExceptionHandler(InvalidProductDescriptionException.class)
    public ProblemDetail handle(
            final InvalidProductDescriptionException exception
    ) {
        return switch (exception.code()) {
            case PRODUCT_DESCRIPTION_REQUIRED -> requiredProblem();
            case PRODUCT_DESCRIPTION_TOO_SHORT -> tooShortProblem();
            case PRODUCT_DESCRIPTION_TOO_LONG -> tooLongProblem();
        };
    }
}
```

exhaustive `switch`는 error-code enum이 추가됐지만 HTTP 매핑이 빠진 경우 컴파일 오류를 발생시킨다. 별도 runtime registry와 시작 시점 validator가 필요하지 않다.

공통 fallback은 가장 낮은 order를 사용하므로 Context Advice는 높은 order로 등록한다.

Context Advice가 `ResponseEntityExceptionHandler`를 상속하는 소비 애플리케이션에서는 starter fallback이 백오프한다. 소비 애플리케이션의 기존 Spring MVC 오류 정책을 보존하기 위한 의도적인 자동 구성 규칙이다.

## 9. 보안 모듈 영향

`security-error-webmvc`도 provider와 catalog를 제거하고 `SecurityExceptionHandler`가 Application 예외를 직접 `ProblemDetail`로 변환하도록 변경했다.

Spring Security 필터의 401/403은 MVC Advice를 통과하지 않으므로 `security-webmvc`의 `SecurityProblemDetailResponseAdapter`가 표준 필드를 직접 직렬화한다.

```text
인증 필요       -> 401 /problems/authentication-required
접근 거부       -> 403 /problems/access-denied
Access Token 오류 -> 401 /problems/invalid-token
Refresh 오류    -> 401 /problems/invalid-refresh-token
내부 보안 계약 오류 -> 500 /problems/security-processing-error
```

내부 예외 message와 token 값은 응답에 사용하지 않는다.

## 10. 테스트 우선 기록

### Red

```bash
./gradlew :modules:error-handling-starter:test \
  --tests com.example.errorhandlingstarter.ErrorHandlingStarterConsumerTest
```

3개 중 2개가 실패했다.

- validation 응답에 기존 `GLOBAL.VALIDATION_ERROR` code가 남아 있었다.
- 500의 type이 기존 `/problems/internal-error`였다.

기존 `ResponseEntityExceptionHandler`와의 공존 테스트도 먼저 fallback Bean이 남아 실패하는 것을 확인한 뒤, 타입 기준 백오프 조건을 추가했다.

```bash
./gradlew :modules:security-error-webmvc:test \
  --tests com.dochiri.security.adapter.in.web.error.SecurityExceptionHandlerTest
```

Production `SecurityExceptionHandler`가 아직 없어 `compileTestJava`가 실패했다.

### Green

다음 대상 검증이 성공했다.

```bash
./gradlew :modules:error-handling-starter:check \
  :modules:security-error-webmvc:check \
  :modules:security-webmvc:check \
  :modules:msa-auth-starter:compileTestJava \
  :modules:msa-gateway-starter:test
```

- starter 테스트 4개 성공
- `security-error-webmvc` 테스트 6개 성공
- `security-webmvc` 테스트 30개 성공
- JPA 없는 실제 security Web MVC 소비 smoke test 성공
- Checkstyle, PMD, SpotBugs, Architecture Convention, JaCoCo 검증 성공

전체 저장소와 변경 코드 커버리지도 통과했다.

```bash
./gradlew clean check -PchangedCoverageBaseRef=origin/main
```

- `clean` 이후 전체 354개 Gradle 작업을 재사용 없이 모두 실행해 성공
- `origin/main` 대비 변경 Production 코드 커버리지 기준 성공

Maven Local에 게시한 공개 `dochiri-service-starter` artifact는 `/Users/seongbin/programming/laboratory/tool-test-lab/starter-probe`의 독립 빌드에서 검증했다.

```bash
./gradlew publishToMavenLocal

cd /Users/seongbin/programming/laboratory/tool-test-lab
./gradlew -p starter-probe clean test --refresh-dependencies
```

- 공개 Maven 좌표와 모든 전이 artifact 해석 성공
- 별도 오류 설정과 `@Import` 없이 애플리케이션 컨텍스트 기동 성공
- H2 소비자 데이터소스로 JPA 자동 구성 성공
- Bean Validation 실패가 내부 validation message를 노출하지 않는 표준 ProblemDetail 400 응답으로 변환됨

소비 프로젝트 본체는 별도 구조 개편 중인 dirty worktree라 이번 라이브러리 검증과 분리했다. probe는 본체 Production/Test source set을 classpath에 넣지 않으므로 공개 artifact의 독립 소비 계약만 검증한다.

## 11. 사용성 및 의도적인 절충

기술 오류만 사용하는 소비자는 starter 의존성 외에 아무 코드도 작성하지 않는다. 비즈니스 오류가 있는 소비자는 자신이 외부에 공개할 의미를 Context Advice 한 곳에 작성한다.

다음 기능은 의도적으로 제공하지 않는다.

- Domain error code로 HTTP 의미 자동 추론
- 별도 문자열 API code
- 자동 trace ID 생성
- 모든 Validation 오류의 고정된 필드 배열
- 오류 계약 중복의 runtime registry 검증

이 기능들이 실제 제품 요구사항이 되면 RFC extension이나 Spring 공식 interceptor를 작은 단위로 추가한다. 가능성만으로 선제 구현하지 않는다.

## 12. 결론

현재 구조는 Clean Architecture 경계를 유지하면서 Spring 공식 기능을 최소한으로 사용한다. 파일 수를 줄이기 위해 책임을 한 클래스에 몰지 않았고, 실제로 독립적인 자동 구성과 fallback 처리만 분리했다.

소비 프로젝트가 알아야 하는 것은 두 가지뿐이다.

1. 애플리케이션 역할에 맞는 공개 starter 하나를 의존한다.
2. 비즈니스 오류의 외부 의미는 해당 Context Web Adapter에서 명시한다.
