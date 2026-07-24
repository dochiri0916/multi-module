package com.dochiri.errorhandling.global.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_TITLE = "서버 오류";
    private static final String INTERNAL_ERROR_DETAIL = "일시적인 오류가 발생했습니다.";

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.error(
                "미처리 예외가 발생했습니다. uri={}, method={}",
                request.getRequestURI(),
                request.getMethod(),
                exception
        );
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_DETAIL
        );
        problem.setTitle(INTERNAL_ERROR_TITLE);
        return problem;
    }
}
