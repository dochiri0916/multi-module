package com.dochiri.security.domain.exception;

public final class InvalidTokenIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidTokenIdException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidTokenIdException required() {
        return new InvalidTokenIdException(SecurityDomainErrorCode.TOKEN_ID_REQUIRED);
    }

    public static InvalidTokenIdException blank() {
        return new InvalidTokenIdException(SecurityDomainErrorCode.TOKEN_ID_BLANK);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
