package com.dochiri.security.domain.exception;

import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenReplayDetectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;
    private final RefreshSessionId affectedSessionId;
    private final TokenId replayedTokenId;
    private final TokenId activeTokenId;

    private RefreshTokenReplayDetectedException(
            SecurityDomainErrorCode errorCode,
            RefreshSessionId sessionId,
            TokenId presentedTokenId,
            TokenId currentTokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.affectedSessionId = sessionId;
        this.replayedTokenId = presentedTokenId;
        this.activeTokenId = currentTokenId;
    }

    public static RefreshTokenReplayDetectedException detected(
            RefreshSessionId sessionId,
            TokenId presentedTokenId,
            TokenId currentTokenId
    ) {
        return new RefreshTokenReplayDetectedException(
                SecurityDomainErrorCode.REFRESH_TOKEN_REPLAYED,
                sessionId,
                presentedTokenId,
                currentTokenId
        );
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return affectedSessionId;
    }

    public TokenId presentedTokenId() {
        return replayedTokenId;
    }

    public TokenId currentTokenId() {
        return activeTokenId;
    }
}
