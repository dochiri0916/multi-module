package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RevokedAt;

@FunctionalInterface
public interface RefreshSessionBulkRevocationPort {

    int revokeAll(AuthenticationSubject subject, RevokedAt revokedAt);
}
