package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.domain.model.CurrentTime;

import java.time.Clock;
import java.time.Instant;

public final class SystemCurrentTimeAdapter implements CurrentTimePort {

    private final Clock clock;

    public SystemCurrentTimeAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CurrentTime currentTime() {
        return new CurrentTime(Instant.now(clock));
    }
}
