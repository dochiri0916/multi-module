package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.EncodedToken;

public interface RefreshTokenVerifierPort {

    DecodedRefreshToken verifyRefresh(EncodedToken refreshToken);
}
