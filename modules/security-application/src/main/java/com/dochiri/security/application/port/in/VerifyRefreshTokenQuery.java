package com.dochiri.security.application.port.in;

import com.dochiri.security.domain.model.EncodedToken;

import java.util.Objects;

public record VerifyRefreshTokenQuery(EncodedToken refreshToken) {

    public VerifyRefreshTokenQuery {
        Objects.requireNonNull(refreshToken, "refreshToken은 필수입니다.");
    }
}
