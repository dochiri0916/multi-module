package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenSubjectMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final TokenId mismatchedTokenId;

    private RefreshTokenSubjectMismatchException(
            SecurityApplicationErrorCode errorCode,
            TokenId tokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.mismatchedTokenId = tokenId;
    }

    public static RefreshTokenSubjectMismatchException subjectMismatch(TokenId tokenId) {
        return new RefreshTokenSubjectMismatchException(
                SecurityApplicationErrorCode.REFRESH_TOKEN_SUBJECT_MISMATCH,
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
