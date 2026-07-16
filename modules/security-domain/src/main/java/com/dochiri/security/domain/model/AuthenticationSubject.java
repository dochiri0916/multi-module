package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationSubjectException;

public record AuthenticationSubject(String value) {

    public AuthenticationSubject {
        if (value == null) {
            throw InvalidAuthenticationSubjectException.required();
        }
        value = value.strip();
        if (value.isBlank()) {
            throw InvalidAuthenticationSubjectException.blank();
        }
    }
}
