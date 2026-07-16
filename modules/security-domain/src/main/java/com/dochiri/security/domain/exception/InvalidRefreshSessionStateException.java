package com.dochiri.security.domain.exception;

import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RefreshSessionStatus;

public final class InvalidRefreshSessionStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;
    private final RefreshSessionId invalidSessionId;
    private final RefreshSessionStatus invalidStatus;

    private InvalidRefreshSessionStateException(
            SecurityDomainErrorCode errorCode,
            RefreshSessionId sessionId,
            RefreshSessionStatus status
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.invalidSessionId = sessionId;
        this.invalidStatus = status;
    }

    public static InvalidRefreshSessionStateException statusRequired() {
        return new InvalidRefreshSessionStateException(
                SecurityDomainErrorCode.REFRESH_SESSION_STATUS_REQUIRED,
                null,
                null
        );
    }

    public static InvalidRefreshSessionStateException revokedAtRequired(RefreshSessionStatus status) {
        return new InvalidRefreshSessionStateException(
                SecurityDomainErrorCode.REFRESH_SESSION_REVOKED_AT_REQUIRED,
                null,
                status
        );
    }

    public static InvalidRefreshSessionStateException activeSessionHasRevokedAt(RefreshSessionStatus status) {
        return new InvalidRefreshSessionStateException(
                SecurityDomainErrorCode.ACTIVE_SESSION_HAS_REVOKED_AT,
                null,
                status
        );
    }

    public static InvalidRefreshSessionStateException inactive(
            RefreshSessionId sessionId,
            RefreshSessionStatus status
    ) {
        return new InvalidRefreshSessionStateException(
                SecurityDomainErrorCode.REFRESH_SESSION_INACTIVE,
                sessionId,
                status
        );
    }

    public static InvalidRefreshSessionStateException unchangedTokenId(RefreshSessionId sessionId) {
        return new InvalidRefreshSessionStateException(
                SecurityDomainErrorCode.REFRESH_TOKEN_ROTATION_ID_UNCHANGED,
                sessionId,
                RefreshSessionStatus.ACTIVE
        );
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return invalidSessionId;
    }

    public RefreshSessionStatus status() {
        return invalidStatus;
    }
}
