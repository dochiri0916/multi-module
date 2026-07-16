package com.dochiri.security.application.service;

import com.dochiri.security.application.port.in.RevokeAllRefreshTokensCommand;
import com.dochiri.security.application.port.in.RevokeAllRefreshTokensResult;
import com.dochiri.security.application.port.in.RevokeAllRefreshTokensUseCase;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshSessionBulkRevocationPort;
import com.dochiri.security.domain.model.RevokedAt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeAllRefreshTokensService implements RevokeAllRefreshTokensUseCase {

    private final RefreshSessionBulkRevocationPort refreshSessionBulkRevocationPort;
    private final CurrentTimePort currentTimePort;

    @Override
    @Transactional
    public RevokeAllRefreshTokensResult execute(RevokeAllRefreshTokensCommand command) {
        RevokedAt revokedAt = new RevokedAt(currentTimePort.currentTime().value());
        int revokedCount = refreshSessionBulkRevocationPort.revokeAll(command.subject(), revokedAt);
        return new RevokeAllRefreshTokensResult(revokedCount);
    }
}
