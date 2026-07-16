package com.dochiri.security.application.exception;

import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;

public final class RefreshTokenReplayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityApplicationErrorCode errorCode;
    private final RefreshSessionId affectedSessionId;
    private final TokenId replayedRefreshTokenId;

    private RefreshTokenReplayException(
            SecurityApplicationErrorCode errorCode,
            RefreshSessionId sessionId,
            TokenId replayedTokenId
    ) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.affectedSessionId = sessionId;
        this.replayedRefreshTokenId = replayedTokenId;
    }

    public static RefreshTokenReplayException replayed(
            RefreshSessionId sessionId,
            TokenId replayedTokenId
    ) {
        return new RefreshTokenReplayException(
                SecurityApplicationErrorCode.REFRESH_TOKEN_REPLAYED,
                sessionId,
                replayedTokenId
        );
    }

    public SecurityApplicationErrorCode code() {
        return errorCode;
    }

    public RefreshSessionId sessionId() {
        return affectedSessionId;
    }

    public TokenId replayedTokenId() {
        return replayedRefreshTokenId;
    }
}
