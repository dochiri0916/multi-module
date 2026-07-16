package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;

import java.util.Optional;

public interface RefreshSessionPort {

    RefreshSession save(RefreshSession refreshSession);

    Optional<RefreshSession> findBySessionIdForUpdate(RefreshSessionId sessionId);

    Optional<RefreshSession> findByCurrentTokenId(TokenId tokenId);
}
