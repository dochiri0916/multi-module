# Error Handling

Spring Boot 4 환경에서는 RFC 9457 Problem Details를 HTTP 에러 응답 계약으로 사용한다.
웹 어댑터는 Spring Framework의 `ProblemDetail`, `ErrorResponse`, `ErrorResponseException`,
`ResponseEntityExceptionHandler`를 중심으로 구현한다.

도메인과 애플리케이션 계층은 Spring Web 타입을 모른다.
`ProblemDetail`, `HttpStatus`, `ErrorResponseException`은 HTTP 어댑터 바깥으로 노출하지 않는다.

참고:

- RFC 9457: https://www.rfc-editor.org/rfc/rfc9457.html
- Spring Framework Error Responses: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Spring Boot MVC Error Handling: https://docs.spring.io/spring-boot/reference/web/servlet.html#error-handling
- Spring Boot WebFlux Error Handling: https://docs.spring.io/spring-boot/reference/web/reactive.html#error-handling

## 목표

- 모든 HTTP API 에러 응답을 `ProblemDetail` JSON으로 통일한다.
- 도메인/애플리케이션 예외는 Spring에 의존하지 않는 에러 코드와 예외로 표현한다.
- 웹 어댑터에서 애플리케이션 예외를 `ProblemDetail` 응답으로 변환한다.
- Spring MVC 내장 예외와 `ErrorResponseException`은 Spring의 `ErrorResponse` 흐름을 따른다.
- 검증 실패, Spring MVC 기본 예외, 미처리 예외도 같은 응답 계약을 갖게 한다.
- 클라이언트가 분기할 수 있는 안정적인 `code`를 제공한다.
- 내부 예외 메시지, 스택트레이스, 외부 시스템 상세 정보는 응답에 노출하지 않는다.

## 응답 계약

기본 응답은 RFC 9457 필드를 따른다.

```json
{
  "type": "/errors/user-not-found",
  "title": "USER_NOT_FOUND",
  "status": 404,
  "detail": "사용자를 찾을 수 없습니다.",
  "instance": "/api/users/1",
  "code": "USER_NOT_FOUND",
  "traceId": "01HX...",
  "userId": 1
}
```

필드 의미:

- `type`: 문제 유형 URI. 도메인 에러는 `/errors/{kebab-case-code}`를 사용한다.
- `title`: 사람이 아닌 시스템 분기용에 가까운 짧은 제목. 기본값은 `ErrorCode.name()`.
- `status`: HTTP status code.
- `detail`: 클라이언트에 노출 가능한 사용자 메시지.
- `instance`: 에러가 발생한 요청 URI.
- `code`: 클라이언트가 안정적으로 분기할 수 있는 애플리케이션 에러 코드.
- `traceId`: 로그 추적용 ID. MDC 또는 `X-Request-Id`에서 가져온다.

`about:blank`는 HTTP 상태 외에 별도 문제 유형이 없을 때만 사용한다.
애플리케이션 에러는 항상 명시적인 `type`을 부여한다.

## 모듈 구조

권장 구조는 에러 계약과 HTTP 표현을 분리하는 것이다.

```text
core 또는 application
├── ErrorCode
├── ApplicationException
├── DomainException
├── 도메인별 DomainErrorCode enum
└── 유스케이스별 ApplicationErrorCode enum

infrastructure adapter
├── InfrastructureException
└── 외부 어댑터별 InfrastructureErrorCode enum

webmvc-error-handling
├── ErrorStatusMapper
├── CommonErrorCode
├── ProblemDetails
├── FieldErrorDetail
└── GlobalExceptionHandler
```

의존 방향:

```text
web adapter -> application -> domain
```

`webmvc-error-handling`은 안쪽 계층의 공개 가능한 에러 코드를 읽어 HTTP `ProblemDetail`로 바꾼다.
반대로 `application`이나 `domain`이 `webmvc-error-handling`에 의존하면 안 된다.

소비 서비스는 도메인/애플리케이션 소유권에 맞는 `ErrorCode` enum과 HTTP 상태 매핑,
`@RestControllerAdvice`를 추가한다.
인프라 에러 코드는 기본적으로 어댑터 내부 구현 세부사항으로 보고 외부 API에 직접 노출하지 않는다.

```java
@RestControllerAdvice
public class ApiExceptionHandler extends GlobalExceptionHandler {
}
```

## ErrorCode

`ErrorCode`는 HTTP 상태를 가지지 않는다.
이 타입은 도메인/애플리케이션 계층에서 사용되므로 Spring Web 의존성이 없어야 한다.

```java
public interface ErrorCode {

    String getMessage();

    String name();
}
```

`ErrorCode` 인터페이스는 하나만 둔다.
enum은 하나로 합치지 않고 소유권에 따라 나눈다.

```text
user/domain/UserDomainErrorCode
user/application/UserApplicationErrorCode
payment/infrastructure/PaymentGatewayErrorCode
```

에러 코드를 나누는 기준:

- `DomainErrorCode`: 엔티티, 값 객체, 도메인 서비스가 지키는 순수 비즈니스 규칙 위반
- `ApplicationErrorCode`: 유스케이스 흐름, 권한, 조회 부재, 중복 요청, 상태 전이 실패
- `InfrastructureErrorCode`: 외부 API, DB, 파일 저장소, 메시지 브로커 같은 기술 실패

예외와 에러 코드의 계층은 맞춰 쓴다.
`DomainException`에는 `DomainErrorCode`, `ApplicationException`에는 `ApplicationErrorCode`를 넣는 것이 기본이다.
인프라 어댑터 내부에서는 `InfrastructureException`과 `InfrastructureErrorCode`를 함께 쓴다.

전역 `CommonErrorCode`는 Spring MVC 기본 예외, validation, 미처리 예외처럼 특정 바운디드 컨텍스트에
속하지 않는 HTTP 공통 오류에만 사용한다.

DDD 기준으로 에러 코드는 바운디드 컨텍스트의 유비쿼터스 언어를 따른다.
`UserApplicationErrorCode.USER_NOT_FOUND`와 `ProjectApplicationErrorCode.PROJECT_NOT_FOUND`처럼
서로 다른 컨텍스트의 같은 문장도 각 컨텍스트가 별도로 소유한다.
공통화는 의미와 정책이 실제로 같은 경우에만 한다.

도메인 에러 코드 예시:

```java
@Getter
public enum UserDomainErrorCode implements ErrorCode {

    USER_ALREADY_DEACTIVATED("이미 탈퇴한 사용자입니다."),
    INVALID_USER_STATUS("사용자 상태가 올바르지 않습니다.");

    private final String message;

    UserDomainErrorCode(String message) {
        this.message = message;
    }
}
```

애플리케이션 에러 코드 예시:

```java
@Getter
public enum UserApplicationErrorCode implements ErrorCode {

    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다."),
    USER_DELETE_NOT_ALLOWED("사용자를 삭제할 수 없습니다.");

    private final String message;

    UserApplicationErrorCode(String message) {
        this.message = message;
    }
}
```

인프라 에러 코드 예시:

```java
@Getter
public enum PaymentGatewayErrorCode implements ErrorCode {

    PAYMENT_GATEWAY_TIMEOUT("결제 서비스 응답이 지연되고 있습니다."),
    PAYMENT_GATEWAY_UNAVAILABLE("결제 서비스를 사용할 수 없습니다.");

    private final String message;

    PaymentGatewayErrorCode(String message) {
        this.message = message;
    }
}
```

인프라 에러 코드는 어댑터 내부에서 로깅, 재시도, 모니터링 분류에 사용할 수 있다.
다만 애플리케이션 유스케이스가 특정 공급자 코드인 `PaymentGatewayErrorCode`나
`BitbucketInfrastructureErrorCode`를 직접 비교하면 의존 방향이 깨진다.
유스케이스가 알아야 하는 실패는 포트 계약의 결과 타입이나 `ApplicationErrorCode`로 표현한다.

조회 결과 없음은 보통 `ApplicationErrorCode`다.
도메인 객체는 DB나 repository 조회 실패를 알지 못한다.
반대로 "이미 탈퇴한 사용자는 재탈퇴할 수 없다" 같은 불변식은 `DomainErrorCode`다.

HTTP 상태는 웹 어댑터에서 별도 매퍼로 결정한다.

```java
public interface ErrorStatusMapper {

    HttpStatusCode statusOf(ErrorCode errorCode);
}
```

예시:

```java
@Component
public class UserErrorStatusMapper implements ErrorStatusMapper {

    @Override
    public HttpStatusCode statusOf(ErrorCode errorCode) {
        if (errorCode == UserApplicationErrorCode.USER_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode == UserApplicationErrorCode.EMAIL_ALREADY_EXISTS) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode == UserDomainErrorCode.USER_ALREADY_DEACTIVATED) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
```

HTTP API만 제공하는 작은 서비스라면 `ErrorCode`가 `HttpStatus`를 직접 가져도 구현은 단순해진다.
하지만 헥사고날 구조와 의존성 최소화를 우선하면 HTTP 상태 매핑은 어댑터 계층으로 분리한다.

## Layer Exceptions

예외도 계층의 언어를 보존한다.

```text
DomainException
ApplicationException
InfrastructureException
```

세 예외는 모두 Spring에 의존하지 않는다.
차이는 "어디에서 발생했고 누가 소유한 실패인가"이다.

- `DomainException`: 도메인 모델이 불변식이나 정책 위반을 발견했을 때 사용한다.
- `ApplicationException`: 유스케이스가 요청을 수행할 수 없을 때 사용한다.
- `InfrastructureException`: 외부 시스템 또는 기술 어댑터 실패를 어댑터 내부에서 표현할 때 사용한다.

공통 기반 클래스를 둘 수 있다.

```java
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> properties;

    protected BusinessException(ErrorCode errorCode, Map<String, Object> properties, Throwable cause) {
        super(Objects.requireNonNull(errorCode).getMessage(), cause);
        this.errorCode = errorCode;
        this.properties = Map.copyOf(properties);
    }
}
```

도메인 예외:

```java
public final class DomainException extends BusinessException {

    public DomainException(ErrorCode errorCode) {
        super(errorCode, Map.of(), null);
    }

    public static DomainException of(ErrorCode errorCode, Object... keyValues) {
        return new DomainException(errorCode, mapArgs(keyValues), null);
    }

    private DomainException(ErrorCode errorCode, Map<String, Object> properties, Throwable cause) {
        super(errorCode, properties, cause);
    }
}
```

애플리케이션 예외:

`ApplicationException`은 유스케이스 흐름의 실패를 표현한다.
Spring의 `ErrorResponseException`을 상속하지 않는다.

```java
@Getter
public final class ApplicationException extends BusinessException {

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public ApplicationException(
            ErrorCode errorCode,
            Map<String, Object> properties,
            Throwable cause
    ) {
        super(errorCode, properties, cause);
    }

    public static ApplicationException of(ErrorCode errorCode, Object... keyValues) {
        return new ApplicationException(errorCode, mapArgs(keyValues), null);
    }
}
```

인프라 예외:

```java
public final class InfrastructureException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> properties;

    public InfrastructureException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Map.of(), cause);
    }

    private InfrastructureException(ErrorCode errorCode, Map<String, Object> properties, Throwable cause) {
        super(Objects.requireNonNull(errorCode).getMessage(), cause);
        this.errorCode = errorCode;
        this.properties = Map.copyOf(properties);
    }
}
```

`InfrastructureException`은 기본적으로 HTTP advice까지 흘려보내지 않는다.
어댑터나 애플리케이션 서비스에서 포트 계약에 맞는 `ApplicationException` 또는 결과 타입으로 바꾼다.
정말 변환하지 못한 인프라 예외는 미처리 예외로 보고 500 공통 응답으로 숨긴다.

사용 예시:

```java
throw new DomainException(UserDomainErrorCode.USER_ALREADY_DEACTIVATED);

throw new ApplicationException(UserApplicationErrorCode.USER_NOT_FOUND);

throw ApplicationException.of(
        UserApplicationErrorCode.USER_NOT_FOUND,
        "userId", userId
);
```

`properties`에는 클라이언트가 복구나 화면 분기에 사용할 수 있는 값만 넣는다.
토큰, 비밀번호, 내부 SQL, 외부 API URL, 스택트레이스, 서버 파일 경로는 넣지 않는다.

도메인 예외에는 가능하면 식별자나 인프라 정보를 넣지 않는다.
도메인 모델의 실패 원인을 설명하는 최소 속성만 허용한다.

## ErrorResponseException

`ErrorResponseException`은 Spring의 `ErrorResponse` 구현체다.
공식 Spring 흐름을 그대로 타야 하는 HTTP 어댑터 내부 예외가 필요할 때만 사용한다.

```java
public final class ApiErrorResponseException extends ErrorResponseException {

    public ApiErrorResponseException(
            ErrorCode errorCode,
            HttpStatusCode statusCode,
            Map<String, Object> properties
    ) {
        super(statusCode, ProblemDetails.from(errorCode, statusCode, properties), null);
    }
}
```

대부분의 애플리케이션 예외는 `ApplicationException`을 던지고,
`GlobalExceptionHandler`에서 `ProblemDetail`로 변환하면 충분하다.
즉, 공식 Spring API는 HTTP 표현 계층에서 사용하고 안쪽 계층으로 전파하지 않는다.

## ProblemDetails

`ProblemDetails`는 `ProblemDetail` 생성을 담당하는 팩토리다.

핵심 규칙:

- `ProblemDetail.forStatusAndDetail(status, message)`로 시작한다.
- `type`은 `/errors/{error-code-kebab-case}`로 설정한다.
- `title`과 `code`는 `ErrorCode.name()`으로 맞춘다.
- `instance`는 요청 URI로 설정한다.
- `traceId`는 MDC의 `traceId`, `trace_id`, 또는 `X-Request-Id`에서 가져온다.
- RFC 9457 기본 필드와 공통 확장 필드는 사용자 정의 property key로 덮어쓸 수 없다.

```java
public static ProblemDetail from(
        ErrorCode errorCode,
        HttpStatusCode statusCode,
        Map<String, Object> properties
) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(
            statusCode,
            errorCode.getMessage()
    );
    body.setType(typeOf(errorCode.name()));
    body.setTitle(errorCode.name());
    body.setProperty("code", errorCode.name());
    addProperties(body, properties);
    return body;
}
```

예약 property key:

```text
type, title, status, detail, instance,
code, traceId, fieldErrors,
errors, timestamp, path, exception, message
```

이 예약어를 막는 이유는 응답 계약이 호출 지점마다 달라지는 것을 방지하기 위해서다.

## GlobalExceptionHandler

전역 핸들러는 `ResponseEntityExceptionHandler`를 상속한다.
Spring MVC가 자체적으로 던지는 예외는 대부분 이 클래스의 protected handler를 통해 들어온다.
공통 핸들러는 `handleExceptionInternal`을 오버라이드해서 Spring이 만든 `ProblemDetail`에도
`code`, `type`, `instance`, `traceId`를 보강한다.

```java
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorStatusMapper errorStatusMapper;

    public GlobalExceptionHandler(ErrorStatusMapper errorStatusMapper) {
        this.errorStatusMapper = errorStatusMapper;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = errorStatusMapper.statusOf(exception.getErrorCode());
        ProblemDetail body = ProblemDetails.from(
                exception.getErrorCode(),
                statusCode,
                exception.getProperties()
        );
        ProblemDetails.applyRequestDetails(body, new ServletWebRequest(request));

        return ResponseEntity
                .status(statusCode)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("미처리 예외가 발생했습니다. uri={}, method={}",
                request.getRequestURI(), request.getMethod(), exception);

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ProblemDetails.internalServerError(new ServletWebRequest(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ProblemDetail problemDetail) {
            body = ProblemDetails.normalize(problemDetail, statusCode, request);
        }

        if (statusCode.is5xxServerError()) {
            log.error("예외를 처리했습니다. status={}, message={}",
                    statusCode.value(), exception.getMessage(), exception);
        } else {
            log.warn("예외를 처리했습니다. status={}, message={}",
                    statusCode.value(), exception.getMessage());
        }

        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }
}
```

`@ExceptionHandler(Exception.class)`는 마지막 안전망으로만 사용한다.
도메인/애플리케이션 예외는 `BusinessException` 전용 handler에서 `ProblemDetail`로 변환한다.
인프라 예외는 포트 경계에서 애플리케이션 예외로 변환하는 것을 원칙으로 한다.
Spring MVC 내장 예외와 `ErrorResponseException`은 `ResponseEntityExceptionHandler`의 공식 흐름으로 처리한다.

## Validation

요청 검증 실패는 하나의 응답 형태로 통일한다.

```json
{
  "type": "/errors/validation-error",
  "title": "VALIDATION_ERROR",
  "status": 400,
  "detail": "요청 값이 올바르지 않습니다.",
  "instance": "/api/users",
  "code": "VALIDATION_ERROR",
  "fieldErrors": [
    {
      "field": "email",
      "rejectedValue": "",
      "reason": "must not be blank",
      "messageCode": "NotBlank"
    }
  ]
}
```

처리 대상:

- `MethodArgumentNotValidException`: `@Valid @RequestBody`
- `HandlerMethodValidationException`: Spring MVC method validation
- `ConstraintViolationException`: `@Validated` 기반 parameter validation
- `BindException`: query/form binding validation

필드 에러 구조:

```java
public record FieldErrorDetail(
        String field,
        Object rejectedValue,
        String reason,
        String messageCode
) {
}
```

`rejectedValue`는 민감 값일 수 있다.
비밀번호, 토큰, 주민번호, 카드번호 등은 마스킹하거나 제외하는 후처리를 추가한다.

## Spring Boot 4 설정

MVC에서 Spring 기본 problem details 응답을 켜려면 다음 설정을 사용할 수 있다.

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

WebFlux 서비스라면 설정 prefix가 다르다.

```yaml
spring:
  webflux:
    problemdetails:
      enabled: true
```

다만 이 프로젝트처럼 `GlobalExceptionHandler`를 등록해 응답 계약을 직접 통제하는 경우,
핵심 계약은 advice 구현과 테스트로 보장한다.
Boot 설정은 Spring 기본 오류 응답 경로까지 Problem Details로 맞추기 위한 보조 설정으로 본다.

## Security 예외

Spring Security의 `AuthenticationEntryPoint`, `AccessDeniedHandler`는 MVC advice를 거치지 않는다.
따라서 401/403도 같은 계약을 쓰려면 security 모듈에서 `ProblemDetail`을 직접 작성해야 한다.

권장 응답:

```json
{
  "type": "/errors/unauthorized",
  "title": "UNAUTHORIZED",
  "status": 401,
  "detail": "인증이 필요합니다.",
  "instance": "/api/users/me",
  "code": "UNAUTHORIZED",
  "traceId": "01HX..."
}
```

security 모듈의 `SecurityResponseWriter`도 `ProblemDetails`와 같은 규칙으로
`code`, `type`, `instance`, `traceId`를 채워야 한다.

## 외부 시스템 예외

외부 API, DB, 메시지 브로커 장애는 다음 기준으로 변환한다.

- 외부 시스템의 404가 유스케이스상 리소스 부재를 의미하면 `ApplicationErrorCode`로 변환한다.
- 외부 시스템 장애, timeout, 5xx는 어댑터 안에서 `InfrastructureErrorCode`로 감싼다.
- 포트 밖으로 나갈 때는 `ApplicationException` 또는 포트가 정의한 결과 타입으로 변환한다.
- 외부 응답 전문, 내부 URL, 인증 헤더, SQL은 응답에 넣지 않고 로그에만 남긴다.

인프라 어댑터 예시:

```java
public class BitbucketProjectAdapter implements ProjectPort {

    @Override
    public ProjectSnapshot getProject(ProjectKey projectKey) {
        try {
            return bitbucketClient.getProject(projectKey.value());
        } catch (BitbucketNotFoundException exception) {
            throw ApplicationException.of(
                    ProjectApplicationErrorCode.PROJECT_NOT_FOUND,
                    "projectKey", projectKey.value()
            );
        } catch (BitbucketTimeoutException exception) {
            InfrastructureException infrastructureException = new InfrastructureException(
                    BitbucketInfrastructureErrorCode.BITBUCKET_TIMEOUT,
                    exception
            );
            throw new ApplicationException(
                    ProjectApplicationErrorCode.PROJECT_TEMPORARILY_UNAVAILABLE,
                    Map.of(),
                    infrastructureException
            );
        }
    }
}
```

포트 결과 타입을 쓰는 예시:

```java
public sealed interface ProjectLookupResult {

    record Found(ProjectSnapshot project) implements ProjectLookupResult {
    }

    record NotFound(ProjectKey projectKey) implements ProjectLookupResult {
    }

    record TemporarilyUnavailable() implements ProjectLookupResult {
    }
}
```

헥사고날 구조에서는 어댑터가 외부 예외를 포트가 정의한 예외나 결과 타입으로 바꾸고,
HTTP 응답 변환은 바깥쪽 advice에서 맡긴다.
작은 HTTP API 서비스라면 어댑터에서 `ApiErrorResponseException`으로 변환하는 것도 가능하지만,
도메인/애플리케이션 모듈이 Spring Web 타입에 직접 의존하지 않도록 경계를 확인한다.

인프라 에러 코드는 가능한 한 외부로 직접 노출하지 않는다.
클라이언트가 알면 안 되는 공급자명, 내부 토폴로지, SQL 상태, 상세 endpoint가 포함될 수 있기 때문이다.
외부 공개 API에서는 유스케이스 의미의 `ApplicationErrorCode`로 변환하는 것을 우선한다.
애플리케이션 유스케이스가 특정 인프라 enum을 import해서 분기하는 구조는 피한다.

## 테스트

필수 테스트:

- `ErrorCode`, `BusinessException`, `DomainException`, `ApplicationException`은 Spring Web 타입에 의존하지 않는다.
- 도메인 에러 코드, 애플리케이션 에러 코드, 인프라 에러 코드는 소유 계층별로 분리된다.
- `GlobalExceptionHandler`가 `BusinessException`을 `ProblemDetail`로 변환한다.
- `ApiErrorResponseException`을 쓰는 경우 `ErrorResponseException`으로서 올바른 `ProblemDetail`을 가진다.
- `ProblemDetails.from`이 `type`, `title`, `status`, `detail`, `code`를 채운다.
- `ErrorStatusMapper`가 공개 가능한 도메인/애플리케이션 `ErrorCode`를 HTTP 상태로 매핑한다.
- 인프라 예외와 인프라 에러 코드는 애플리케이션 유스케이스가 직접 import하지 않는다.
- 인프라 실패는 포트 경계에서 애플리케이션 예외 또는 결과 타입으로 변환된다.
- 예약 property key를 넣으면 실패한다.
- validation 실패가 `fieldErrors`를 반환한다.
- Spring MVC 기본 예외가 `handleExceptionInternal`에서 normalize된다.
- 미처리 예외는 500과 공통 메시지를 반환하고 내부 메시지를 숨긴다.
- 5xx는 stack trace와 함께 error 로그, 4xx는 warn 로그로 남긴다.

MockMvc 예시:

```java
mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"email": ""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("/errors/validation-error"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
```

## 적용 순서

1. `ErrorCode`, `BusinessException`, `DomainException`, `ApplicationException`은 Spring Web 의존성이 없는 안쪽 모듈에 둔다.
2. 에러 코드 enum은 `DomainErrorCode`, `ApplicationErrorCode`, `InfrastructureErrorCode`로 소유 계층에 맞춰 나눈다.
3. `ProblemDetails`, `FieldErrorDetail`, `GlobalExceptionHandler`, `ErrorStatusMapper`는 Web MVC 어댑터 모듈에 둔다.
4. 소비 서비스는 공개 가능한 도메인/애플리케이션 에러 코드별 HTTP 상태 매핑을 만든다.
5. 도메인 규칙 위반은 `DomainException`, 유스케이스 실패는 `ApplicationException`으로 던진다.
6. 외부 시스템 실패는 어댑터 내부에서 `InfrastructureException`으로 감싸고, 포트 경계에서 `ApplicationException` 또는 결과 타입으로 변환한다.
7. `GlobalExceptionHandler`에서 `BusinessException`을 `ProblemDetail`로 변환한다.
8. validation, Spring MVC 기본 예외, 미처리 예외를 MockMvc 테스트로 고정한다.
9. Spring Security 401/403도 같은 `ProblemDetail` 계약으로 맞춘다.
10. OpenAPI 문서에 공통 에러 스키마와 공개 가능한 `code` 목록을 노출한다.

## 운영 원칙

- 클라이언트 분기는 HTTP status가 아니라 `code`를 우선 사용한다.
- `detail`은 사용자에게 보여도 되는 문장만 사용한다.
- 로그에는 내부 원인을 남기고, 응답에는 복구 가능한 정보만 담는다.
- 에러 코드 이름은 한 번 배포하면 호환성 계약으로 취급한다.
- 전역 enum 하나에 모든 에러 코드를 모으지 않는다.
- 에러 코드는 바운디드 컨텍스트와 계층의 소유권을 기준으로 나눈다.
- 인프라 에러 코드는 기본적으로 내부 원인에 가깝게 취급하고, 공개 API에는 애플리케이션 에러로 변환해 노출한다.
- 애플리케이션 유스케이스가 특정 인프라 어댑터의 에러 enum에 의존하지 않는다.
- `fieldErrors` 외의 확장 필드는 문제 타입별로 명확한 이름을 사용한다.
- 범용 `data`, `payload`, `meta` 같은 확장 필드는 사용하지 않는다.
