package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupCountException;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsCommand;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsResult;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsUseCase;
import com.dochiri.security.application.port.out.RefreshSessionCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class CleanupRefreshSessionsService implements CleanupRefreshSessionsUseCase {

    private final RefreshSessionCleanupPort refreshSessionCleanupPort;

    @Override
    @Transactional
    public CleanupRefreshSessionsResult execute(CleanupRefreshSessionsCommand command) {
        int deletedCount = refreshSessionCleanupPort.deleteBatch(
                command.expiredBefore(),
                command.revokedBefore(),
                command.batchSize()
        );
        if (deletedCount < 0 || deletedCount > command.batchSize()) {
            throw InvalidRefreshSessionCleanupCountException.invalid(deletedCount);
        }
        return new CleanupRefreshSessionsResult(
                deletedCount,
                deletedCount == command.batchSize()
        );
    }
}
