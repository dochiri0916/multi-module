package com.dochiri.security.application.port.in;

import com.dochiri.security.application.exception.InvalidRefreshTokenRevocationCountException;

public record RevokeAllRefreshTokensResult(int revokedCount) {

    public RevokeAllRefreshTokensResult {
        if (revokedCount < 0) {
            throw InvalidRefreshTokenRevocationCountException.negative(revokedCount);
        }
    }
}
