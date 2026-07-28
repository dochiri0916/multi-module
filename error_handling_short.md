# Error Handling 공식 최소 적용안

## 결론

자체 오류 처리 프레임워크를 만들지 않고 RFC 9457과 Spring이 공식 제공하는 기능만 사용한다.

```text
Domain/Application Exception
             |
             v
Context Web Adapter의 @ExceptionHandler
             |
             v
Spring ProblemDetail
```

Domain/Application은 실패 의미만 표현하고, Web Adapter가 HTTP 표현으로 변환한다.

## 설치와 실제 구성

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-service-starter:1.0.0'
}
```

Gateway는 `dochiri-msa-gateway-starter`, 인증 서버는 `dochiri-msa-auth-starter`를 사용한다. 내부 error-handling artifact를 직접 선언하지 않는다. 별도 engine 모듈은 없으며 내부 starter의 Production Java 코드는 두 개다.

```text
ErrorHandlingAutoConfiguration.java
GlobalExceptionHandler.java
```

소비 애플리케이션에 기존 `ResponseEntityExceptionHandler` Bean이 있으면 starter의 공통 Advice는 등록되지 않는다. 기존 handler와 충돌하지 않고 소비 애플리케이션의 오류 정책을 그대로 따른다.

## 공식 기준

RFC 9457의 표준 필드는 다음과 같다.

- `type`: 문제 유형을 식별하는 URI
- `title`: 문제 유형에 대한 짧고 안정적인 설명
- `status`: HTTP 상태 코드
- `detail`: 해당 오류 발생에 대한 설명
- `instance`: 해당 오류가 발생한 요청 또는 발생 건 식별자

`code`, `traceId`, `fieldErrors`는 필수가 아닌 확장 필드다. 공식 최소 구성에서는 별도 `code`를 만들지 않고 `type` URI를 문제 유형의 식별자로 사용한다.

Spring이 공식 제공하는 다음 기능만 사용한다.

- `ProblemDetail`
- `ErrorResponse`
- `ErrorResponseException`
- `ResponseEntityExceptionHandler`
- `@RestControllerAdvice`
- `@ExceptionHandler`

## Domain에는 실패 의미만 선언

```java
package com.example.product.domain.exception;

public enum ProductDomainErrorCode {
    PRODUCT_DESCRIPTION_REQUIRED,
    PRODUCT_DESCRIPTION_TOO_SHORT,
    PRODUCT_DESCRIPTION_TOO_LONG
}
```

```java
package com.example.product.domain.exception;

import static java.util.Objects.requireNonNull;

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

    public static InvalidProductDescriptionException tooShort() {
        return new InvalidProductDescriptionException(
                ProductDomainErrorCode.PRODUCT_DESCRIPTION_TOO_SHORT
        );
    }

    public static InvalidProductDescriptionException tooLong() {
        return new InvalidProductDescriptionException(
                ProductDomainErrorCode.PRODUCT_DESCRIPTION_TOO_LONG
        );
    }

    public ProductDomainErrorCode code() {
        return errorCode;
    }
}
```

Domain 예외에는 다음 정보를 넣지 않는다.

- `HttpStatus`
- `ProblemDetail`
- `ErrorResponse`
- 사용자 노출 title/detail

`super(errorCode.name())`은 내부 식별용일 뿐이며 API 응답에 노출하지 않는다.

## Web Adapter에서 직접 변환

별도 Contributor, Registry, Provider 또는 API 오류 enum을 만들지 않는다. 각 Context의 Web Adapter가 자신이 소유한 예외를 `ProblemDetail`로 직접 변환한다.

```java
package com.example.product.adapter.in.web;

import com.example.product.domain.exception.InvalidProductDescriptionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

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

같은 예외가 여러 error code를 가진다면 exhaustive `switch`로 변환한다. 새 enum 상수가 추가되고 Web 매핑이 누락되면 컴파일 오류가 발생한다.

예외마다 HTTP 상태가 다르다면 `problem` 메서드가 `HttpStatus`를 받도록 확장한다. 실제 길이처럼 클라이언트가 처리해야 하는 구조화된 값이 있을 때만 RFC 9457 확장 필드로 추가한다.

## 응답 예시

```json
{
  "type": "/problems/product-description-required",
  "title": "상품 설명 입력 오류",
  "status": 400,
  "detail": "상품 설명은 필수입니다.",
  "instance": "/api/products"
}
```

`title`은 동일한 `type`에서 안정적으로 유지한다. `detail`은 클라이언트가 문제를 수정하는 데 필요한 내용만 포함한다. `exception.getMessage()`, stack trace, SQL, 내부 클래스명과 같은 구현 정보는 노출하지 않는다.

## Spring MVC 기본 예외

starter는 `ResponseEntityExceptionHandler`를 상속한 공통 Advice를 자동 등록하므로 별도 설정이 필요 없다. starter를 사용하지 않고 Spring Boot 기본 기능만 사용할 때는 다음 설정으로 Problem Details를 활성화할 수 있다.

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

Spring Boot가 Spring MVC의 기본 `ErrorResponse` 예외를 RFC 9457 응답으로 처리한다.

Bean Validation의 필드별 오류 배열은 RFC 9457의 필수 형식이 아니다. 클라이언트 요구사항이 생기기 전에는 별도 `fieldErrors` 규격과 변환기를 만들지 않는다.

## 공통 모듈의 최소 책임

공식 최소 기준에서는 `error-handling-starter`가 비즈니스 예외를 알거나 자동 추론하지 않는다.

starter의 책임은 다음으로 한정한다.

- Spring MVC와 Bean Validation 의존성을 제공하는 starter
- Spring MVC 기본 예외의 `ProblemDetail` 변환
- 내부 정보가 노출되지 않는 공통 500 fallback

비즈니스 예외의 HTTP status, type, title, detail은 소비 프로젝트의 Context Web Adapter가 결정한다. Spring은 Domain error code만으로 이 값을 자동 추론할 수 없으므로 이 매핑은 생략할 수 없다.

## 만들지 않는 구성요소

공식 최소 구성에서는 다음 자체 추상화를 만들지 않는다.

- `ApiErrorContributor`
- `ApiErrorDefinition`
- `ApiErrorCode`
- `ApiErrorMapping`
- `ApiErrorMessage`
- `ProblemType`
- Mapping/Message Provider
- Registry, Catalog, ContractValidator
- package 이름 기반 API code 생성

Domain 예외에 `@ResponseStatus`를 붙이거나 `ErrorResponse`를 구현하면 코드는 더 짧아지지만 Domain이 HTTP와 Spring에 결합된다. Clean Architecture 경계를 유지하려면 Context Web Adapter의 명시적 `@ExceptionHandler`가 가장 단순한 선택이다.

## 참고 자료

- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Spring Framework: Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Framework: Exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
- [Spring Boot: Servlet Web Applications](https://docs.spring.io/spring-boot/reference/web/servlet.html)
