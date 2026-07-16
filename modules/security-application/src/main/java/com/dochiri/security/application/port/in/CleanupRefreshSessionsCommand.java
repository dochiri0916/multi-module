package com.dochiri.security.application.port.in;

import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupBatchSizeException;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RevokedAt;

import java.util.Objects;

public record CleanupRefreshSessionsCommand(
        CurrentTime expiredBefore,
        RevokedAt revokedBefore,
        int batchSize
) {

    private static final int MAXIMUM_BATCH_SIZE = 1_000;

    public CleanupRefreshSessionsCommand {
        Objects.requireNonNull(expiredBefore, "expiredBefore는 필수입니다.");
        Objects.requireNonNull(revokedBefore, "revokedBefore는 필수입니다.");
        if (batchSize <= 0 || batchSize > MAXIMUM_BATCH_SIZE) {
            throw InvalidRefreshSessionCleanupBatchSizeException.invalid(batchSize);
        }
    }
}
