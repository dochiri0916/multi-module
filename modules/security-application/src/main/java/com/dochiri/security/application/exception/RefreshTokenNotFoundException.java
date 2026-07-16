package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final TokenId missingTokenId;

    private RefreshTokenNotFoundException(
            SecurityApplicationErrorCode errorCode,
            TokenId tokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.missingTokenId = tokenId;
    }

    public static RefreshTokenNotFoundException tokenNotFound(TokenId tokenId) {
        return new RefreshTokenNotFoundException(SecurityApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND, tokenId);
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public TokenId tokenId() {
        return missingTokenId;
    }
}
