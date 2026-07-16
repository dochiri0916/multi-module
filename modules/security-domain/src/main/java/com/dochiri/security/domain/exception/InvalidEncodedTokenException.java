package com.dochiri.security.domain.exception;

public final class InvalidEncodedTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidEncodedTokenException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidEncodedTokenException required() {
        return new InvalidEncodedTokenException(SecurityDomainErrorCode.ENCODED_TOKEN_REQUIRED);
    }

    public static InvalidEncodedTokenException blank() {
        return new InvalidEncodedTokenException(SecurityDomainErrorCode.ENCODED_TOKEN_BLANK);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
