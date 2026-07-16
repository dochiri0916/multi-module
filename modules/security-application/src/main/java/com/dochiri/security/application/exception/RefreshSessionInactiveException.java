package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RefreshSessionStatus;

public final class RefreshSessionInactiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final RefreshSessionId inactiveSessionId;
    private final RefreshSessionStatus inactiveStatus;

    private RefreshSessionInactiveException(
            SecurityApplicationErrorCode errorCode,
            RefreshSessionId sessionId,
            RefreshSessionStatus status
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.inactiveSessionId = sessionId;
        this.inactiveStatus = status;
    }

    public static RefreshSessionInactiveException inactive(
            RefreshSessionId sessionId,
            RefreshSessionStatus status
    ) {
        return new RefreshSessionInactiveException(
                SecurityApplicationErrorCode.REFRESH_SESSION_INACTIVE,
                sessionId,
                status
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return inactiveSessionId;
    }

    public RefreshSessionStatus status() {
        return inactiveStatus;
    }
}
