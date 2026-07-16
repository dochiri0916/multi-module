package com.dochiri.security.domain.exception;

public final class InvalidTokenExpirationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidTokenExpirationException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidTokenExpirationException required() {
        return new InvalidTokenExpirationException(SecurityDomainErrorCode.TOKEN_EXPIRATION_REQUIRED);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
