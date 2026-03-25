package com.dochiri.sample.hexagonal.support.error;

import com.dochiri.errorhandling.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler extends GlobalExceptionHandler {
}
