package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.domain.model.TokenId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class UuidTokenIdGeneratorAdapter implements TokenIdGeneratorPort {

    @Override
    public TokenId generate() {
        return new TokenId(UUID.randomUUID().toString().replace("-", ""));
    }
}
