package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

import java.util.Objects;

public record DecodedRefreshSessionToken(
        RefreshSessionId sessionId,
        AuthenticationSubject subject,
        AuthenticationRole role,
        TokenId tokenId,
        TokenExpiration expiresAt
) {
    public DecodedRefreshSessionToken {
        Objects.requireNonNull(sessionId, "sessionId는 필수입니다.");
        Objects.requireNonNull(subject, "subject는 필수입니다.");
        Objects.requireNonNull(role, "role은 필수입니다.");
        Objects.requireNonNull(tokenId, "tokenId는 필수입니다.");
        Objects.requireNonNull(expiresAt, "expiresAt은 필수입니다.");
    }
}
