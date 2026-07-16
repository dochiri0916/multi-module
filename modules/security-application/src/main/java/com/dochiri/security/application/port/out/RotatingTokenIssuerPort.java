package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

public interface RotatingTokenIssuerPort extends TokenIssuerPort {

    IssuedTokenPair rotate(
            AuthenticationSubject subject,
            AuthenticationRole role,
            RefreshSessionId sessionId,
            TokenId refreshTokenId,
            TokenExpiration refreshTokenExpiresAt,
            CurrentTime issuedAt
    );
}
