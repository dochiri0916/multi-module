package com.dochiri.security.application.exception;

public final class InvalidRefreshTokenRevocationCountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final int invalidRevokedCount;

    private InvalidRefreshTokenRevocationCountException(
            SecurityApplicationErrorCode errorCode,
            int revokedCount
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.invalidRevokedCount = revokedCount;
    }

    public static InvalidRefreshTokenRevocationCountException negative(int revokedCount) {
        return new InvalidRefreshTokenRevocationCountException(
                SecurityApplicationErrorCode.REFRESH_TOKEN_REVOCATION_COUNT_INVALID,
                revokedCount
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public int revokedCount() {
        return invalidRevokedCount;
    }
}
