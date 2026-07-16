package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.TokenId;

@FunctionalInterface
public interface TokenIdGeneratorPort {

    TokenId generate();
}
