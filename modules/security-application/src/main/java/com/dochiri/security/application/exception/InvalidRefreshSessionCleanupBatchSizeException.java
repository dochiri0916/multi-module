package com.dochiri.security.application.exception;

public final class InvalidRefreshSessionCleanupBatchSizeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final int invalidBatchSize;

    private InvalidRefreshSessionCleanupBatchSizeException(
            SecurityApplicationErrorCode errorCode,
            int batchSize
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.invalidBatchSize = batchSize;
    }

    public static InvalidRefreshSessionCleanupBatchSizeException invalid(int batchSize) {
        return new InvalidRefreshSessionCleanupBatchSizeException(
                SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID,
                batchSize
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public int batchSize() {
        return invalidBatchSize;
    }
}
