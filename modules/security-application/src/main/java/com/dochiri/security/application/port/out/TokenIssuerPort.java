package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.TokenId;

@FunctionalInterface
public interface TokenIssuerPort {

    IssuedTokenPair issue(
            AuthenticationSubject subject,
            AuthenticationRole role,
            TokenId refreshTokenId,
            CurrentTime issuedAt
    );
}
