package com.dochiri.security.application.exception;

public final class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;

    private InvalidTokenException(SecurityApplicationErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidTokenException expired() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_EXPIRED);
    }

    public static InvalidTokenException malformed() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_MALFORMED);
    }

    public static InvalidTokenException invalidCategory() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID);
    }

    public static InvalidTokenException missingSubject() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING);
    }

    public static InvalidTokenException missingRole() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_ROLE_MISSING);
    }

    public static InvalidTokenException missingTokenId() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_ID_MISSING);
    }

    public static InvalidTokenException missingExpiration() {
        return new InvalidTokenException(SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING);
    }

    public static InvalidTokenException missingRefreshSessionId() {
        return new InvalidTokenException(SecurityApplicationErrorCode.REFRESH_SESSION_ID_MISSING);
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }
}
