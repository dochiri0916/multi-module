package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidRefreshSessionIdException;

import java.io.Serializable;
import java.util.UUID;

public record RefreshSessionId(String value) implements Serializable {

    public RefreshSessionId {
        if (value == null) {
            throw InvalidRefreshSessionIdException.required();
        }
        value = value.strip();
        if (value.isBlank()) {
            throw InvalidRefreshSessionIdException.blank();
        }
    }

    public static RefreshSessionId generate() {
        return new RefreshSessionId(UUID.randomUUID().toString().replace("-", ""));
    }
}
