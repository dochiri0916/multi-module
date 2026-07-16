package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.EncodedToken;

public interface AccessTokenVerifierPort {

    DecodedAccessToken verifyAccess(EncodedToken accessToken);
}
