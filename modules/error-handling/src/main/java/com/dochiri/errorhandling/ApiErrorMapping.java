package com.dochiri.errorhandling;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public record ApiErrorMapping(HttpStatus status, ProblemType problemType) {

    public ApiErrorMapping {
        Objects.requireNonNull(status, "status는 필수입니다.");
        Objects.requireNonNull(problemType, "problemType은 필수입니다.");
    }
}
