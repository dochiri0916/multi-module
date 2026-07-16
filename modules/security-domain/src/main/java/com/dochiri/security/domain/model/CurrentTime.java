package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidCurrentTimeException;

import java.time.Instant;

public record CurrentTime(Instant value) {

    public CurrentTime {
        if (value == null) {
            throw InvalidCurrentTimeException.required();
        }
    }
}
