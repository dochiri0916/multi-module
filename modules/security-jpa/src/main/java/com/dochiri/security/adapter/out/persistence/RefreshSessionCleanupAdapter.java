package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.application.port.out.RefreshSessionCleanupPort;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RevokedAt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshSessionCleanupAdapter implements RefreshSessionCleanupPort {

    private final RefreshSessionJpaRepository refreshSessionJpaRepository;

    @Override
    public int deleteBatch(CurrentTime expiredBefore, RevokedAt revokedBefore, int batchSize) {
        return refreshSessionJpaRepository.deleteCleanupBatch(
                expiredBefore.value(),
                revokedBefore.value(),
                batchSize
        );
    }
}
