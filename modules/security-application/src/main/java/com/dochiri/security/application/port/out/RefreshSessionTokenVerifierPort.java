package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.EncodedToken;

public interface RefreshSessionTokenVerifierPort extends RefreshTokenVerifierPort {

    DecodedRefreshSessionToken verifyRefreshSession(EncodedToken refreshToken);
}
