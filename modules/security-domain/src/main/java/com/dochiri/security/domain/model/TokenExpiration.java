package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidTokenExpirationException;

import java.time.Instant;

public record TokenExpiration(Instant value) {

    public TokenExpiration {
        if (value == null) {
            throw InvalidTokenExpirationException.required();
        }
    }

    public boolean isExpiredAt(CurrentTime currentTime) {
        return !value.isAfter(currentTime.value());
    }
}
