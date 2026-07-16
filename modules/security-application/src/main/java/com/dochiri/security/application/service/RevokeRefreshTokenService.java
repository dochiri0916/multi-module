package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.port.in.RevokeRefreshTokenCommand;
import com.dochiri.security.application.port.in.RevokeRefreshTokenResult;
import com.dochiri.security.application.port.in.RevokeRefreshTokenUseCase;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionPort;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RevokedAt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public final class RevokeRefreshTokenService implements RevokeRefreshTokenUseCase {

    private final RefreshTokenVerifierPort refreshTokenVerifierPort;
    private final RefreshSessionPort refreshSessionRepositoryPort;
    private final CurrentTimePort currentTimePort;

    @Override
    @Transactional
    public RevokeRefreshTokenResult execute(RevokeRefreshTokenCommand command) {
        DecodedRefreshToken decodedToken = refreshTokenVerifierPort.verifyRefresh(command.refreshToken());
        Optional<RefreshSession> storedSession = refreshSessionRepositoryPort
                .findByCurrentTokenId(decodedToken.tokenId());
        if (storedSession.isEmpty()) {
            return new RevokeRefreshTokenResult(false);
        }

        RefreshSession session = storedSession.get();
        if (!session.subject().equals(decodedToken.subject())) {
            throw RefreshTokenSubjectMismatchException.subjectMismatch(session.currentTokenId());
        }
        if (session.status().isRevoked()) {
            return new RevokeRefreshTokenResult(false);
        }

        RefreshSession revokedSession = session.revoke(new RevokedAt(currentTimePort.currentTime().value()));
        refreshSessionRepositoryPort.save(revokedSession);
        return new RevokeRefreshTokenResult(true);
    }
}
