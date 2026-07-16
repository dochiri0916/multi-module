package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidTokenIdException;

import java.io.Serializable;
import java.util.UUID;

public record TokenId(String value) implements Serializable {

    public TokenId {
        if (value == null) {
            throw InvalidTokenIdException.required();
        }
        value = value.strip();
        if (value.isBlank()) {
            throw InvalidTokenIdException.blank();
        }
    }

    public static TokenId generate() {
        return new TokenId(UUID.randomUUID().toString().replace("-", ""));
    }
}
