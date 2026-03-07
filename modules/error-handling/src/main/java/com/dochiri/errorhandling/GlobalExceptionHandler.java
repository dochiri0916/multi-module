package com.dochiri.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception exception, HttpServletRequest request) {
        log.error("미처리 예외가 발생했습니다. uri={}, method={}", request.getRequestURI(), request.getMethod(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createProblemDetail(
                        exception,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "일시적인 오류가 발생했습니다.",
                        null, null,
                        new ServletWebRequest(request)
                ));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (statusCode.is5xxServerError()) {
            log.error("예외를 처리했습니다. status={}, message={}", statusCode.value(), exception.getMessage(), exception);
            return super.handleExceptionInternal(exception, body, headers, statusCode, request);
        }

        log.warn("예외를 처리했습니다. status={}, message={}", statusCode.value(), exception.getMessage());
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }
}
