package com.dochiri.security.application.exception;

public final class InvalidRefreshSessionCleanupCountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final int invalidDeletedCount;

    private InvalidRefreshSessionCleanupCountException(
            SecurityApplicationErrorCode errorCode,
            int deletedCount
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.invalidDeletedCount = deletedCount;
    }

    public static InvalidRefreshSessionCleanupCountException invalid(int deletedCount) {
        return new InvalidRefreshSessionCleanupCountException(
                SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID,
                deletedCount
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public int deletedCount() {
        return invalidDeletedCount;
    }
}
