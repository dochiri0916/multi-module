package com.dochiri.sample.hexagonal.support.error;

import com.dochiri.errorhandling.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SampleErrorCode implements ErrorCode {
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부서를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 사용자 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    SampleErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
