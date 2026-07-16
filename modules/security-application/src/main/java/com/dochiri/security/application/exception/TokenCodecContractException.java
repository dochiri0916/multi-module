package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.TokenId;

public final class TokenCodecContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final TokenId expectedTokenId;

    private TokenCodecContractException(
            SecurityApplicationErrorCode errorCode,
            TokenId expectedTokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.expectedTokenId = expectedTokenId;
    }

    public static TokenCodecContractException unexpectedTokenId(TokenId expectedTokenId) {
        return new TokenCodecContractException(
                SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION,
                expectedTokenId
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public TokenId tokenId() {
        return expectedTokenId;
    }
}
