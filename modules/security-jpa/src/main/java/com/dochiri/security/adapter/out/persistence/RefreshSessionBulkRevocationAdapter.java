package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.application.port.out.RefreshSessionBulkRevocationPort;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RevokedAt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshSessionBulkRevocationAdapter implements RefreshSessionBulkRevocationPort {

    private final RefreshSessionJpaRepository refreshSessionJpaRepository;

    @Override
    public int revokeAll(AuthenticationSubject subject, RevokedAt revokedAt) {
        return refreshSessionJpaRepository.revokeAllActiveBySubject(subject.value(), revokedAt.value());
    }
}
