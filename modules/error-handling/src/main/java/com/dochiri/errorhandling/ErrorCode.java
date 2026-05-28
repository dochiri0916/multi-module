package com.dochiri.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public interface ErrorCode {

    HttpStatus getHttpStatus();

    default HttpStatusCode getStatusCode() {
        return getHttpStatus();
    }

    String getMessage();

    String name();

}
