package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.domain.model.TokenId;

import java.util.UUID;

public final class UuidTokenIdGeneratorAdapter implements TokenIdGeneratorPort {

    @Override
    public TokenId generate() {
        return new TokenId(UUID.randomUUID().toString().replace("-", ""));
    }
}
