package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenExpirationMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final TokenId mismatchedTokenId;

    private RefreshTokenExpirationMismatchException(
            SecurityApplicationErrorCode errorCode,
            TokenId tokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.mismatchedTokenId = tokenId;
    }

    public static RefreshTokenExpirationMismatchException expirationMismatch(TokenId tokenId) {
        return new RefreshTokenExpirationMismatchException(
                SecurityApplicationErrorCode.REFRESH_TOKEN_EXPIRATION_MISMATCH,
                tokenId
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public TokenId tokenId() {
        return mismatchedTokenId;
    }
}
