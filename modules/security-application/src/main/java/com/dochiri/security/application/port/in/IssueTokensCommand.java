package com.dochiri.security.application.port.in;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;

import java.util.Objects;

public record IssueTokensCommand(
        AuthenticationSubject subject,
        AuthenticationRole role
) {
    public IssueTokensCommand {
        Objects.requireNonNull(subject, "subject는 필수입니다.");
        Objects.requireNonNull(role, "role은 필수입니다.");
    }
}
