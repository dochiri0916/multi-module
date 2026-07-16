package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidRevokedAtException;

import java.time.Instant;

public record RevokedAt(Instant value) {

    public RevokedAt {
        if (value == null) {
            throw InvalidRevokedAtException.required();
        }
    }
}
