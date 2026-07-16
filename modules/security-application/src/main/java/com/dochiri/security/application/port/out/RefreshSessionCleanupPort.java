package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RevokedAt;

@FunctionalInterface
public interface RefreshSessionCleanupPort {

    int deleteBatch(CurrentTime expiredBefore, RevokedAt revokedBefore, int batchSize);
}
