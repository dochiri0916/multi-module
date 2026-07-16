package com.dochiri.security.application.port.in;

import com.dochiri.security.domain.model.AuthenticationSubject;

import java.util.Objects;

public record RevokeAllRefreshTokensCommand(AuthenticationSubject subject) {

    public RevokeAllRefreshTokensCommand {
        Objects.requireNonNull(subject, "subject는 필수입니다.");
    }
}
