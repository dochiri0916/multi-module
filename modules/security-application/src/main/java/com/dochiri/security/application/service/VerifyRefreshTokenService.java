package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshTokenExpirationMismatchException;
import com.dochiri.security.application.exception.RefreshTokenInactiveException;
import com.dochiri.security.application.exception.RefreshTokenNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenRoleMismatchException;
import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.port.in.VerifyRefreshTokenQuery;
import com.dochiri.security.application.port.in.VerifyRefreshTokenResult;
import com.dochiri.security.application.port.in.VerifyRefreshTokenUseCase;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionPort;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshTokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class VerifyRefreshTokenService implements VerifyRefreshTokenUseCase {

    private final RefreshTokenVerifierPort refreshTokenVerifierPort;
    private final RefreshSessionPort refreshSessionRepositoryPort;
    private final CurrentTimePort currentTimePort;

    @Override
    @Transactional(readOnly = true)
    public VerifyRefreshTokenResult execute(VerifyRefreshTokenQuery query) {
        DecodedRefreshToken decodedToken = refreshTokenVerifierPort.verifyRefresh(query.refreshToken());
        RefreshSession storedSession = refreshSessionRepositoryPort.findByCurrentTokenId(decodedToken.tokenId())
                .orElseThrow(() -> RefreshTokenNotFoundException.tokenNotFound(decodedToken.tokenId()));
        validateContract(storedSession, decodedToken);
        if (!storedSession.isActiveAt(currentTimePort.currentTime())) {
            throw RefreshTokenInactiveException.inactive(
                    storedSession.currentTokenId(),
                    RefreshTokenStatus.valueOf(storedSession.status().name())
            );
        }
        return new VerifyRefreshTokenResult(
                storedSession.subject(),
                storedSession.currentTokenId(),
                storedSession.expiresAt()
        );
    }

    private void validateContract(RefreshSession storedSession, DecodedRefreshToken decodedToken) {
        if (!storedSession.subject().equals(decodedToken.subject())) {
            throw RefreshTokenSubjectMismatchException.subjectMismatch(storedSession.currentTokenId());
        }
        if (!storedSession.role().equals(decodedToken.role())) {
            throw RefreshTokenRoleMismatchException.roleMismatch(storedSession.sessionId());
        }
        if (!storedSession.expiresAt().equals(decodedToken.expiresAt())) {
            throw RefreshTokenExpirationMismatchException.expirationMismatch(storedSession.currentTokenId());
        }
    }
}
