package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.CurrentTime;

@FunctionalInterface
public interface CurrentTimePort {

    CurrentTime currentTime();
}
