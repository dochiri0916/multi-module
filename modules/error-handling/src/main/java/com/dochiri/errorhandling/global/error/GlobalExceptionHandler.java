package com.dochiri.errorhandling.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApiProblemDetailFactory problemDetailFactory;
    private final ApiExceptionMapper exceptionMapper;
    private final ApiErrorMessageCatalog messageCatalog;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception exception, HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        if (exception instanceof RuntimeException runtimeException) {
            return exceptionMapper.map(runtimeException)
                    .map(mappedError -> responseFor(mappedError, webRequest))
                    .orElseGet(() -> unexpectedResponse(exception, request, webRequest));
        }
        return unexpectedResponse(exception, request, webRequest);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("요청 값 검증에 실패했습니다. uri={}, method={}, errorCount={}",
                request.getRequestURI(), request.getMethod(), exception.getConstraintViolations().size());
        ProblemDetail body = validationProblemDetail(
                fieldErrorsFromConstraintViolations(exception.getConstraintViolations()),
                new ServletWebRequest(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException exception, HttpServletRequest request) {
        log.warn("요청 값 바인딩 검증에 실패했습니다. uri={}, method={}, errorCount={}",
                request.getRequestURI(), request.getMethod(), exception.getBindingResult().getErrorCount());
        ProblemDetail body = validationProblemDetail(
                fieldErrorsFromBinding(exception.getBindingResult().getAllErrors()),
                new ServletWebRequest(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        ProblemDetail body = validationProblemDetail(
                fieldErrorsFromBinding(exception.getBindingResult().getAllErrors()),
                request
        );
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        ProblemDetail body = validationProblemDetail(fieldErrorsFromMethodValidation(exception), request);
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        Object responseBody = body;
        HttpStatusCode responseStatus = statusCode;
        if (!hasApiCode(body)) {
            MappedApiError mappedError = globalMappedError(globalErrorCodeFor(statusCode));
            responseBody = problemDetail(mappedError, request);
            responseStatus = mappedError.mapping().status();
        }

        if (responseStatus.is5xxServerError()) {
            log.error("예외를 처리했습니다. status={}, exception={}",
                    responseStatus.value(), exception.getClass().getName(), exception);
        } else {
            log.warn("예외를 처리했습니다. status={}, exception={}",
                    responseStatus.value(), exception.getClass().getSimpleName());
        }
        return super.handleExceptionInternal(exception, responseBody, headers, responseStatus, request);
    }

    private ResponseEntity<Object> unexpectedResponse(
            Exception exception,
            HttpServletRequest request,
            WebRequest webRequest
    ) {
        log.error("미처리 예외가 발생했습니다. uri={}, method={}",
                request.getRequestURI(), request.getMethod(), exception);
        return responseFor(globalMappedError(GlobalErrorCode.INTERNAL_SERVER_ERROR), webRequest);
    }

    private ResponseEntity<Object> responseFor(MappedApiError mappedError, WebRequest request) {
        return ResponseEntity
                .status(mappedError.mapping().status())
                .body(problemDetail(mappedError, request));
    }

    private ProblemDetail validationProblemDetail(List<FieldErrorDetail> fieldErrors, WebRequest request) {
        MappedApiError mappedError = globalMappedError(GlobalErrorCode.VALIDATION_ERROR);
        ApiErrorMessage message = messageCatalog.messageFor(mappedError.code());
        return problemDetailFactory.createValidation(mappedError, message, fieldErrors, request);
    }

    private ProblemDetail problemDetail(MappedApiError mappedError, WebRequest request) {
        ApiErrorMessage message = messageCatalog.messageFor(mappedError.code());
        return problemDetailFactory.create(mappedError, message, request);
    }

    private MappedApiError globalMappedError(GlobalErrorCode errorCode) {
        ApiErrorCode code = ApiErrorCode.from(errorCode);
        return MappedApiError.from(code, exceptionMapper.mappingFor(code));
    }

    private GlobalErrorCode globalErrorCodeFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 404 -> GlobalErrorCode.NOT_FOUND;
            case 405 -> GlobalErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> statusCode.is5xxServerError()
                    ? GlobalErrorCode.INTERNAL_SERVER_ERROR
                    : GlobalErrorCode.BAD_REQUEST;
        };
    }

    private boolean hasApiCode(Object body) {
        if (!(body instanceof ProblemDetail problemDetail)) {
            return false;
        }
        Map<String, Object> properties = problemDetail.getProperties();
        return properties != null && properties.containsKey(ApiProblemDetailFactory.CODE);
    }

    private List<FieldErrorDetail> fieldErrorsFromBinding(List<ObjectError> errors) {
        return errors.stream()
                .map(this::fieldErrorFromObjectError)
                .toList();
    }

    private FieldErrorDetail fieldErrorFromObjectError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return new FieldErrorDetail(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getCode()
            );
        }
        return new FieldErrorDetail(
                error.getObjectName(),
                error.getDefaultMessage(),
                error.getCode()
        );
    }

    private List<FieldErrorDetail> fieldErrorsFromMethodValidation(HandlerMethodValidationException exception) {
        List<FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                fieldErrors.addAll(fieldErrorsFromBinding(parameterErrors.getAllErrors()));
                continue;
            }

            String field = requestFieldName(result);
            if (field == null) {
                field = "arg" + result.getMethodParameter().getParameterIndex();
            }

            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fieldErrors.add(new FieldErrorDetail(
                        field,
                        error.getDefaultMessage(),
                        messageCode(error)
                ));
            }
        }
        return fieldErrors;
    }

    private List<FieldErrorDetail> fieldErrorsFromConstraintViolations(
            Iterable<ConstraintViolation<?>> constraintViolations
    ) {
        List<FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : constraintViolations) {
            fieldErrors.add(new FieldErrorDetail(
                    violation.getPropertyPath().toString(),
                    violation.getMessage(),
                    violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
            ));
        }
        return fieldErrors;
    }

    private String messageCode(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        if (codes == null || codes.length == 0) {
            return null;
        }
        String code = codes[codes.length - 1];
        int dotIndex = code.indexOf('.');
        if (dotIndex < 0) {
            return code;
        }
        return code.substring(0, dotIndex);
    }

    private String requestFieldName(ParameterValidationResult result) {
        RequestParam requestParam = result.getMethodParameter().getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            if (hasText(requestParam.name())) {
                return requestParam.name();
            }
            if (hasText(requestParam.value())) {
                return requestParam.value();
            }
        }
        return result.getMethodParameter().getParameterName();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
