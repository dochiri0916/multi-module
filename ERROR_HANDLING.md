# Error Handling

`modules:error-handling`은 RFC 9457 기반 HTTP 오류 응답을 제공하는 Spring Boot Web MVC Adapter 모듈이다.
Domain/Application 예외는 소비 Context가 소유하고, 이 모듈은 예외를 HTTP 표현으로 변환하는 공통 흐름만 제공한다.

## 책임

| 구성요소 | 책임 |
| --- | --- |
| `ApiErrorCode` | Context enum을 namespace가 포함된 안정 API code로 변환 |
| `ApiErrorMapping` | HTTP status와 `ProblemType` 표현 |
| `ErrorCodeMappingProvider` | Context 예외와 error code의 HTTP 매핑 |
| `ApiExceptionMapper` | 등록된 provider를 조합해 예외 매핑 |
| `ApiErrorMessage` | 사용자 노출 title/detail |
| `ApiErrorMessageProvider` | Context별 한국어 메시지 catalog |
| `ApiErrorMessageCatalog` | 등록된 메시지 provider의 단일 조회 지점 |
| `ApiProblemDetailFactory` | 공통 `ProblemDetail` 생성과 안전한 속성 보강 |
| `ApiErrorContractValidator` | 매핑·메시지의 중복과 누락을 시작 시 검증 |
| `GlobalExceptionHandler` | Spring MVC, validation, 매핑된 예외, 미처리 예외 처리 |
| `ErrorHandlingAutoConfiguration` | 공통 bean과 handler 자동 구성 |

이 모듈은 다음 계약을 제공하지 않는다.

- Domain/Application 공통 예외 상속 타입
- Domain/Application이 구현해야 하는 error code interface
- 예외 내부의 HTTP status나 사용자 메시지
- 예외가 임의 응답 속성을 직접 전달하는 API

## 의존 방향

Domain/Application은 `dochiri-error-handling`을 의존하지 않는다.
각 Context의 Web Adapter만 provider 계약을 구현한다.

```text
consumer.domain <- consumer.application <- consumer.adapter.in.web
                                             |
                                             v
                              dochiri-error-handling
```

## Domain/Application 예외

예외는 Spring, HTTP status, `ProblemDetail`을 모르는 plain unchecked exception으로 작성한다.
생성자는 private으로 숨기고 의미 있는 정적 팩토리만 공개한다.

```java
public enum MemberApplicationErrorCode {
    MEMBER_NOT_FOUND
}
```

```java
import static java.util.Objects.requireNonNull;

public final class MemberNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final MemberApplicationErrorCode errorCode;
    private final MemberId missingMemberId;

    private MemberNotFoundException(
            final MemberApplicationErrorCode errorCode,
            final MemberId memberId
    ) {
        super(requireNonNull(errorCode).name());
        this.errorCode = errorCode;
        this.missingMemberId = requireNonNull(memberId);
    }

    public static MemberNotFoundException memberNotFound(final MemberId memberId) {
        return new MemberNotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND, memberId);
    }

    public MemberApplicationErrorCode code() {
        return errorCode;
    }

    public MemberId memberId() {
        return missingMemberId;
    }
}
```

예외의 `getMessage()`는 API detail로 사용하지 않는다.

## 안정 API code

모든 provider key는 `ApiErrorCode.from(errorCode)`로 생성한다.

```java
ApiErrorCode code = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
```

결과는 다음과 같다.

```text
MEMBER.NOT_FOUND
```

formatter는 `domain`, `application`, `adapter` package segment 바로 앞의 Context 이름을 namespace로 사용한다.
enum 상수에 동일한 Context prefix가 있으면 한 번만 표현한다.

```text
com.example.member.application.exception.MemberApplicationErrorCode
MEMBER_NOT_FOUND
-> MEMBER.NOT_FOUND
```

전역 오류는 `GLOBAL.*` namespace를 사용한다.
provider의 Map key에 `"MEMBER.NOT_FOUND"` 같은 문자열을 직접 작성하지 않는다.

## Context HTTP 매핑 provider

Context의 Web Adapter가 예외 타입, HTTP status, problem type, 공개 가능한 속성을 결정한다.

```java
@Component
public final class MemberErrorCodeMappingProvider implements ErrorCodeMappingProvider {

    private static final Map<ApiErrorCode, ApiErrorMapping> ERROR_CODE_MAPPINGS = Map.of(
            ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND),
            new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND)
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMapping> errorCodeMappings() {
        return Map.copyOf(ERROR_CODE_MAPPINGS);
    }

    @Override
    public Optional<MappedApiError> resolve(final RuntimeException exception) {
        if (exception instanceof MemberNotFoundException memberNotFoundException) {
            return resolve(memberNotFoundException.code())
                    .map(mappedError -> mappedError.withProperties(Map.of(
                            "memberId",
                            memberNotFoundException.memberId().value()
                    )));
        }
        return Optional.empty();
    }
}
```

공개 속성은 Web Adapter가 안전성을 검토한 값만 선택한다.
비밀번호, access/refresh token, API key, SQL, 외부 endpoint, stack trace는 전달하지 않는다.

## Context 메시지 provider

사용자 노출 title/detail은 같은 Context의 message provider만 소유한다.

```java
@Component
public final class MemberErrorMessageProvider implements ApiErrorMessageProvider {

    private static final Map<ApiErrorCode, ApiErrorMessage> ERROR_MESSAGES = Map.of(
            ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND),
            new ApiErrorMessage("회원 조회 실패", "회원을 찾을 수 없습니다.")
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMessage> errorMessages() {
        return Map.copyOf(ERROR_MESSAGES);
    }
}
```

메시지는 한국어로 작성하며 예외 문자열을 그대로 사용하지 않는다.

## 자동 구성과 완전성 검증

Spring Boot는 다음 공통 bean을 자동 구성한다.

- `GlobalErrorCodeMappingProvider`
- `GlobalErrorMessageProvider`
- `ApiExceptionMapper`
- `ApiErrorMessageCatalog`
- `ApiProblemDetailFactory`
- `ApiErrorContractValidator`
- `GlobalExceptionHandler`

소비 프로젝트는 Context provider를 `@Component`로 등록하면 된다.
별도의 Context별 `@RestControllerAdvice`나 `*ContextConfig`는 만들지 않는다.

애플리케이션 시작 시 다음 오류 계약을 검증한다.

- 동일 API code의 HTTP 매핑 중복
- 동일 API code의 사용자 메시지 중복
- HTTP 매핑만 있고 메시지가 없는 code
- 메시지만 있고 HTTP 매핑이 없는 code

검증 실패 시 애플리케이션 시작이 실패한다.

### Security 오류 provider

`dochiri-security`는 같은 확장 계약으로 인증·인가 오류 provider를 등록한다.

```text
SECURITY.AUTHENTICATION_REQUIRED -> 401 /problems/unauthorized
SECURITY.ACCESS_DENIED           -> 403 /problems/forbidden
```

Spring Security handler는 좁은 `SecurityErrorResponsePort`만 호출하고, 기본 adapter가 이 모듈의 mapper, message catalog와 `ApiProblemDetailFactory`를 조합한다. 따라서 보안 오류도 일반 Web Adapter 오류와 같은 `code`, `traceId`, `instance` 계약 및 시작 시 완전성 검증을 적용받는다.

## ProblemDetail 계약

```json
{
  "type": "/problems/not-found",
  "title": "회원 조회 실패",
  "status": 404,
  "detail": "회원을 찾을 수 없습니다.",
  "instance": "/api/members/member-id",
  "code": "MEMBER.NOT_FOUND",
  "traceId": "01HX...",
  "memberId": "member-id"
}
```

- `type`: HTTP 매핑 provider가 선택한 `ProblemType`
- `title`, `detail`: message catalog가 제공한 한국어 메시지
- `code`: `ApiErrorCode` formatter가 만든 안정 code
- `instance`: 요청 URI
- `traceId`: MDC `traceId`, `trace_id`, 또는 `X-Request-Id`

다음 property key는 Context provider가 덮어쓸 수 없다.

```text
type, title, status, detail, instance,
code, traceId, fieldErrors,
errors, timestamp, path, exception, message
```

## 전역 오류

`GlobalErrorCode`는 enum 상수만 가지며 HTTP status나 사용자 메시지를 보관하지 않는다.

```text
GLOBAL.INTERNAL_SERVER_ERROR
GLOBAL.VALIDATION_ERROR
GLOBAL.BAD_REQUEST
GLOBAL.NOT_FOUND
GLOBAL.METHOD_NOT_ALLOWED
GLOBAL.UNSUPPORTED_MEDIA_TYPE
```

HTTP 표현은 `GlobalErrorCodeMappingProvider`, 메시지는 `GlobalErrorMessageProvider`가 각각 소유한다.

## Validation

다음 validation 예외를 지원한다.

- `MethodArgumentNotValidException`
- `HandlerMethodValidationException`
- `ConstraintViolationException`
- `BindException`

필드 오류는 원본 입력을 포함하지 않는다.

```java
public record FieldErrorDetail(
        String field,
        String reason,
        String messageCode
) {
}
```

```json
{
  "type": "/problems/validation-failed",
  "title": "요청 검증 실패",
  "status": 400,
  "detail": "요청 값이 올바르지 않습니다.",
  "code": "GLOBAL.VALIDATION_ERROR",
  "fieldErrors": [
    {
      "field": "email",
      "reason": "올바른 형식의 이메일 주소여야 합니다.",
      "messageCode": "Email"
    }
  ]
}
```

Validation 4xx 로그와 응답에는 rejected value, 비밀번호, token 원문을 기록하지 않는다.

## 마이그레이션

| 이전 API | 대체 API |
| --- | --- |
| `CommonErrorCode`의 status/message | `GlobalErrorCodeMappingProvider` + `GlobalErrorMessageProvider` |
| `ProblemDetails.from(...)` | provider 등록 후 `ApiProblemDetailFactory`를 사용하는 공통 handler |
| Context별 `@ExceptionHandler` | `ErrorCodeMappingProvider.resolve(...)` |
| Context별 메시지 catalog 메서드 | `ApiErrorMessageProvider.errorMessages()` |
| 문자열 API code 상수 | `ApiErrorCode.from(contextErrorCode)` |
| code를 그대로 title로 사용 | `ApiErrorMessage.title()` |
| 보안 handler의 별도 `SecurityResponseWriter` | `SecurityErrorResponsePort`와 공통 `ApiProblemDetailFactory` |

H-01에서 제거된 `BaseException`과 공통 `ErrorCode`도 다시 제공하지 않는다.
기존 `CommonErrorCode`, `ProblemDetails`, 직접 상속형 `GlobalExceptionHandler` 사용 코드는 호환되지 않는다.

## 검증

```bash
./gradlew :modules:error-handling:test
./gradlew check
```

주요 회귀 테스트는 다음을 확인한다.

- `MEMBER.NOT_FOUND`, `GLOBAL.INTERNAL_SERVER_ERROR` namespace 생성
- plain Application 예외의 Context provider 매핑
- HTTP 매핑과 사용자 메시지의 중복·누락 거부
- Spring Boot 자동 구성과 시작 시 계약 검증
- 한국어 title/detail과 problem type 적용
- 예약 property 덮어쓰기 차단
- validation 응답과 로그의 민감한 원본 값 비노출
