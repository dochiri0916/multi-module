package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.domain.model.CurrentTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public final class SystemCurrentTimeAdapter implements CurrentTimePort {

    private final Clock clock;

    @Override
    public CurrentTime currentTime() {
        return new CurrentTime(Instant.now(clock));
    }
}
