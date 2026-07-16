package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.RefreshSessionId;

public final class RefreshSessionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final RefreshSessionId missingSessionId;

    private RefreshSessionNotFoundException(
            SecurityApplicationErrorCode errorCode,
            RefreshSessionId sessionId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.missingSessionId = sessionId;
    }

    public static RefreshSessionNotFoundException sessionNotFound(RefreshSessionId sessionId) {
        return new RefreshSessionNotFoundException(
                SecurityApplicationErrorCode.REFRESH_SESSION_NOT_FOUND,
                sessionId
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return missingSessionId;
    }
}
