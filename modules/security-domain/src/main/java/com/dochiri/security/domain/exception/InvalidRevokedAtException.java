package com.dochiri.security.domain.exception;

public final class InvalidRevokedAtException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidRevokedAtException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidRevokedAtException required() {
        return new InvalidRevokedAtException(SecurityDomainErrorCode.REVOKED_AT_REQUIRED);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
