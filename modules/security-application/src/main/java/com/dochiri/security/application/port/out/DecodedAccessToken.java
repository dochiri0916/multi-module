package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.TokenExpiration;

import java.util.Objects;

public record DecodedAccessToken(
        AuthenticationSubject subject,
        AuthenticationRole role,
        TokenExpiration expiresAt
) {
    public DecodedAccessToken {
        Objects.requireNonNull(subject, "subject는 필수입니다.");
        Objects.requireNonNull(role, "role은 필수입니다.");
        Objects.requireNonNull(expiresAt, "expiresAt은 필수입니다.");
    }
}
