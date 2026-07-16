package com.dochiri.security.adapter.in.web.authentication;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;

import java.security.Principal;
import java.util.Objects;

public record JwtPrincipal(
        AuthenticationSubject subject,
        AuthenticationRole role
) implements Principal {
    public JwtPrincipal {
        Objects.requireNonNull(subject, "subject는 필수입니다.");
        Objects.requireNonNull(role, "role은 필수입니다.");
    }

    @Override
    public String getName() {
        return subject.value();
    }
}
