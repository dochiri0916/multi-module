package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

import java.util.Objects;

public record IssuedTokenPair(
        EncodedToken accessToken,
        EncodedToken refreshToken,
        TokenId refreshTokenId,
        TokenExpiration refreshTokenExpiresAt
) {
    public IssuedTokenPair {
        Objects.requireNonNull(accessToken, "accessToken은 필수입니다.");
        Objects.requireNonNull(refreshToken, "refreshToken은 필수입니다.");
        Objects.requireNonNull(refreshTokenId, "refreshTokenId는 필수입니다.");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt은 필수입니다.");
    }
}
