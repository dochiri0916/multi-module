package com.dochiri.security.application.port.in;

import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

import java.util.Objects;

public record VerifyRefreshTokenResult(
        AuthenticationSubject subject,
        TokenId tokenId,
        TokenExpiration expiresAt
) {
    public VerifyRefreshTokenResult {
        Objects.requireNonNull(subject, "subject는 필수입니다.");
        Objects.requireNonNull(tokenId, "tokenId는 필수입니다.");
        Objects.requireNonNull(expiresAt, "expiresAt은 필수입니다.");
    }
}
