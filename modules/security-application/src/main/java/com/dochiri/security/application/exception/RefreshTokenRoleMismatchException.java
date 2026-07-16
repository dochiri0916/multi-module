package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.RefreshSessionId;

public final class RefreshTokenRoleMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final RefreshSessionId mismatchedSessionId;

    private RefreshTokenRoleMismatchException(
            SecurityApplicationErrorCode errorCode,
            RefreshSessionId sessionId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.mismatchedSessionId = sessionId;
    }

    public static RefreshTokenRoleMismatchException roleMismatch(RefreshSessionId sessionId) {
        return new RefreshTokenRoleMismatchException(
                SecurityApplicationErrorCode.REFRESH_TOKEN_ROLE_MISMATCH,
                sessionId
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return mismatchedSessionId;
    }
}
