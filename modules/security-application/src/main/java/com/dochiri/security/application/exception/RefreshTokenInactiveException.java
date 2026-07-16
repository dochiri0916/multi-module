package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.RefreshTokenStatus;
import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenInactiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final TokenId inactiveTokenId;
    private final RefreshTokenStatus inactiveStatus;

    private RefreshTokenInactiveException(
            SecurityApplicationErrorCode errorCode,
            TokenId tokenId,
            RefreshTokenStatus status
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.inactiveTokenId = tokenId;
        this.inactiveStatus = status;
    }

    public static RefreshTokenInactiveException inactive(TokenId tokenId, RefreshTokenStatus status) {
        return new RefreshTokenInactiveException(SecurityApplicationErrorCode.REFRESH_TOKEN_INACTIVE, tokenId, status);
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public TokenId tokenId() {
        return inactiveTokenId;
    }

    public RefreshTokenStatus status() {
        return inactiveStatus;
    }
}
