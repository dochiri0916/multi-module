package com.dochiri.security.domain.exception;

public final class InvalidRefreshSessionIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidRefreshSessionIdException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidRefreshSessionIdException required() {
        return new InvalidRefreshSessionIdException(SecurityDomainErrorCode.REFRESH_SESSION_ID_REQUIRED);
    }

    public static InvalidRefreshSessionIdException blank() {
        return new InvalidRefreshSessionIdException(SecurityDomainErrorCode.REFRESH_SESSION_ID_BLANK);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
