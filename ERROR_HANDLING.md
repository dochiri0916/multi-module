# Error Handling

내부 `error-handling-starter`는 Spring Boot Web MVC 서비스에 Spring 공식 `ProblemDetail` 처리를 자동 구성한다. 별도 오류 engine, registry, provider 또는 message catalog를 제공하지 않는다.

## 설치

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-service-starter:1.0.0'
}
```

Gateway는 `dochiri-msa-gateway-starter`, 인증 서버는 `dochiri-msa-auth-starter`를 사용한다. 오류 처리 내부 artifact를 별도로 선언하지 않는다.

starter의 Production Java 코드는 두 개다.

```text
ErrorHandlingAutoConfiguration.java
GlobalExceptionHandler.java
```

- `ErrorHandlingAutoConfiguration`: Servlet 환경에 공통 Advice를 자동 등록한다.
- `GlobalExceptionHandler`: `ResponseEntityExceptionHandler`로 Spring MVC 기본 예외를 처리하고 미처리 예외를 안전한 500으로 변환한다.

Web MVC와 Hibernate Validator는 starter의 전이 의존성으로 제공된다.

소비 애플리케이션이 이미 `ResponseEntityExceptionHandler` Bean을 제공하면 공통 Advice는 자동으로 물러난다. 기존 전역 오류 처리와 중복 등록되지 않으며, 그 애플리케이션의 handler가 Spring MVC 오류와 fallback 정책을 계속 소유한다.

## 응답 규격

RFC 9457의 표준 필드만 기본으로 사용한다.

```json
{
  "type": "/problems/product-description-required",
  "title": "상품 설명 입력 오류",
  "status": 400,
  "detail": "상품 설명은 필수입니다.",
  "instance": "/api/products"
}
```

- `type`: 클라이언트가 문제 유형을 식별할 URI
- `title`: 같은 문제 유형에서 안정적으로 유지할 요약
- `status`: 실제 HTTP 응답과 같은 상태
- `detail`: 클라이언트가 문제를 수정하는 데 필요한 설명
- `instance`: Spring이 설정하는 요청 경로

별도 `code`, `traceId`, `fieldErrors`는 기본 응답에 추가하지 않는다. 일반적인 HTTP 오류는 `about:blank`를 사용하며 Spring 직렬화 결과에서 `type`이 생략될 수 있다.

## Domain과 Web Adapter 경계

Domain/Application 예외는 HTTP 상태, `ProblemDetail`, 사용자 메시지를 알지 못한다. 실패 의미의 enum과 필요한 상태만 보관한다.

```java
public enum ProductDomainErrorCode {
    PRODUCT_DESCRIPTION_REQUIRED,
    PRODUCT_DESCRIPTION_TOO_SHORT,
    PRODUCT_DESCRIPTION_TOO_LONG
}
```

```java
public final class InvalidProductDescriptionException extends RuntimeException {

    private final ProductDomainErrorCode errorCode;

    private InvalidProductDescriptionException(final ProductDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = requireNonNull(errorCode);
    }

    public static InvalidProductDescriptionException required() {
        return new InvalidProductDescriptionException(
                ProductDomainErrorCode.PRODUCT_DESCRIPTION_REQUIRED
        );
    }

    public ProductDomainErrorCode code() {
        return errorCode;
    }
}
```

`getMessage()`는 API 응답에 사용하지 않는다.

## 비즈니스 예외 변환

라이브러리는 비즈니스 error code의 HTTP 의미를 추론하지 않는다. 각 Context의 Web Adapter가 Spring 공식 `@ExceptionHandler`로 직접 변환한다.

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class ProductExceptionHandler {

    @ExceptionHandler(InvalidProductDescriptionException.class)
    public ProblemDetail handle(
            final InvalidProductDescriptionException exception
    ) {
        return switch (exception.code()) {
            case PRODUCT_DESCRIPTION_REQUIRED -> problem(
                    "product-description-required",
                    "상품 설명 입력 오류",
                    "상품 설명은 필수입니다."
            );
            case PRODUCT_DESCRIPTION_TOO_SHORT -> problem(
                    "product-description-too-short",
                    "상품 설명 입력 오류",
                    "상품 설명이 최소 길이보다 짧습니다."
            );
            case PRODUCT_DESCRIPTION_TOO_LONG -> problem(
                    "product-description-too-long",
                    "상품 설명 입력 오류",
                    "상품 설명이 최대 길이를 초과했습니다."
            );
        };
    }

    private ProblemDetail problem(
            final String type,
            final String title,
            final String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        return problem;
    }
}
```

Context Advice는 공통 fallback보다 먼저 선택되도록 높은 order를 사용한다. 같은 예외가 여러 error code를 가질 때는 exhaustive `switch`가 신규 상수의 매핑 누락을 컴파일 단계에서 드러낸다.

Context Advice가 `ResponseEntityExceptionHandler`까지 상속해야 한다면 해당 Advice가 전역 handler 역할도 맡는다. 이 경우 starter의 공통 Advice는 자동 구성되지 않는다.

## 공통 fallback

처리되지 않은 예외는 다음 안전한 응답으로 변환하고 원본 예외는 서버 로그에 남긴다.

```json
{
  "title": "서버 오류",
  "status": 500,
  "detail": "일시적인 오류가 발생했습니다.",
  "instance": "/api/example"
}
```

예외 message, stack trace, SQL, token, 내부 클래스명은 응답에 포함하지 않는다.

## Validation

`ResponseEntityExceptionHandler`의 공식 동작을 사용한다. custom constraint message와 rejected value를 별도 확장 필드로 복사하지 않는다. 필드별 오류 배열이 실제 API 요구사항이 될 때만 소비 Web Adapter에서 별도 problem type과 extension을 정의한다.

## 보안 오류

`security-error-webmvc`는 보안 Application 예외를 Context Advice에서 직접 변환한다. Spring Security 필터 밖에서 발생하는 401/403은 `security-webmvc`의 `SecurityProblemDetailResponseAdapter`가 같은 표준 필드를 직접 작성한다.

```text
인증 필요  -> 401 /problems/authentication-required
접근 거부  -> 403 /problems/access-denied
토큰 오류  -> 401 /problems/invalid-token
```

## 검증

```bash
./gradlew :modules:error-handling-starter:check
./gradlew :modules:security-error-webmvc:check
./gradlew :modules:security-webmvc:check
./gradlew check
```

실제 Maven artifact 소비 검증은 `publishToMavenLocal` 후 별도 Spring Boot 프로젝트에서 수행한다.

## 참고 자료

- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Spring Framework: Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Framework: Exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
