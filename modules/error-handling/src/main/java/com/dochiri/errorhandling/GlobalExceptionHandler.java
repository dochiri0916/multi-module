package com.dochiri.errorhandling;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception exception, HttpServletRequest request) {
        log.error("미처리 예외가 발생했습니다. uri={}, method={}", request.getRequestURI(), request.getMethod(), exception);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ProblemDetails.internalServerError(new ServletWebRequest(request)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("요청 값 검증에 실패했습니다. uri={}, method={}, message={}",
                request.getRequestURI(), request.getMethod(), exception.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ProblemDetails.validationError(
                        fieldErrorsFromConstraintViolations(exception.getConstraintViolations()),
                        new ServletWebRequest(request)
                ));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException exception, HttpServletRequest request) {
        log.warn("요청 값 바인딩 검증에 실패했습니다. uri={}, method={}, message={}",
                request.getRequestURI(), request.getMethod(), exception.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ProblemDetails.validationError(
                        fieldErrorsFromBinding(exception.getBindingResult().getAllErrors()),
                        new ServletWebRequest(request)
                ));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        ProblemDetail body = ProblemDetails.validationError(
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
        ProblemDetail body = ProblemDetails.validationError(fieldErrorsFromMethodValidation(exception), request);
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
        if (body instanceof ProblemDetail problemDetail) {
            body = ProblemDetails.normalize(problemDetail, statusCode, request);
        }

        if (statusCode.is5xxServerError()) {
            log.error("예외를 처리했습니다. status={}, message={}", statusCode.value(), exception.getMessage(), exception);
            return super.handleExceptionInternal(exception, body, headers, statusCode, request);
        }

        log.warn("예외를 처리했습니다. status={}, message={}", statusCode.value(), exception.getMessage());
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
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
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage(),
                    fieldError.getCode()
            );
        }

        return new FieldErrorDetail(
                error.getObjectName(),
                null,
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
                        result.getArgument(),
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
                    violation.getInvalidValue(),
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
