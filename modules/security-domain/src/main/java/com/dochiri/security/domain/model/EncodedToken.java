package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidEncodedTokenException;

public record EncodedToken(String value) {

    public EncodedToken {
        if (value == null) {
            throw InvalidEncodedTokenException.required();
        }
        value = value.strip();
        if (value.isBlank()) {
            throw InvalidEncodedTokenException.blank();
        }
    }

    @Override
    public String toString() {
        return "EncodedToken[REDACTED]";
    }
}
