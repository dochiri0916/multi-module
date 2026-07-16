package com.dochiri.security.application.port.in;

import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.TokenExpiration;

import java.util.Objects;

public record RotateTokensResult(
        EncodedToken accessToken,
        EncodedToken refreshToken,
        TokenExpiration refreshTokenExpiresAt
) {
    public RotateTokensResult {
        Objects.requireNonNull(accessToken, "accessToken은 필수입니다.");
        Objects.requireNonNull(refreshToken, "refreshToken은 필수입니다.");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt은 필수입니다.");
    }
}
