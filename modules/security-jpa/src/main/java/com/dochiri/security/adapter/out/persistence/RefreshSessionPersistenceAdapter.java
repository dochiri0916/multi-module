package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshSessionPersistenceAdapter implements RefreshSessionRepositoryPort {

    private final RefreshSessionJpaRepository refreshSessionJpaRepository;

    @Override
    public RefreshSession save(RefreshSession refreshSession) {
        RefreshSessionEntity entity = refreshSessionJpaRepository
                .findBySessionId(refreshSession.sessionId().value())
                .map(storedEntity -> {
                    RefreshSessionMapper.updateEntity(refreshSession, storedEntity);
                    return storedEntity;
                })
                .orElseGet(() -> RefreshSessionMapper.toEntity(refreshSession));
        return RefreshSessionMapper.toDomain(refreshSessionJpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshSession> findBySessionIdForUpdate(RefreshSessionId sessionId) {
        return refreshSessionJpaRepository.findBySessionIdForUpdate(sessionId.value())
                .map(RefreshSessionMapper::toDomain);
    }

    @Override
    public Optional<RefreshSession> findByCurrentTokenId(TokenId tokenId) {
        return refreshSessionJpaRepository.findByCurrentTokenId(tokenId.value())
                .map(RefreshSessionMapper::toDomain);
    }
}
