package com.dochiri.security.domain.exception;

public final class InvalidCurrentTimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidCurrentTimeException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidCurrentTimeException required() {
        return new InvalidCurrentTimeException(SecurityDomainErrorCode.CURRENT_TIME_REQUIRED);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
