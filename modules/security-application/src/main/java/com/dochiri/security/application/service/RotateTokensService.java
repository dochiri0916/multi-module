package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.RefreshSessionInactiveException;
import com.dochiri.security.application.exception.RefreshSessionNotFoundException;
import com.dochiri.security.application.exception.RefreshTokenExpirationMismatchException;
import com.dochiri.security.application.exception.RefreshTokenReplayException;
import com.dochiri.security.application.exception.RefreshTokenRoleMismatchException;
import com.dochiri.security.application.exception.RefreshTokenSubjectMismatchException;
import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.application.port.in.RotateTokensCommand;
import com.dochiri.security.application.port.in.RotateTokensResult;
import com.dochiri.security.application.port.in.RotateTokensUseCase;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.DecodedRefreshSessionToken;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.domain.exception.RefreshTokenReplayDetectedException;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RotateTokensService implements RotateTokensUseCase {

    private final RotatingTokenIssuerPort tokenIssuerPort;
    private final RefreshSessionTokenVerifierPort refreshTokenVerifierPort;
    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final TokenIdGeneratorPort tokenIdGeneratorPort;
    private final CurrentTimePort currentTimePort;

    @Override
    @Transactional(noRollbackFor = RefreshTokenReplayException.class)
    public RotateTokensResult execute(RotateTokensCommand command) {
        DecodedRefreshSessionToken decodedToken = refreshTokenVerifierPort.verifyRefreshSession(command.refreshToken());
        RefreshSession storedSession = refreshSessionRepositoryPort
                .findBySessionIdForUpdate(decodedToken.sessionId())
                .orElseThrow(() -> RefreshSessionNotFoundException.sessionNotFound(decodedToken.sessionId()));
        validateContract(storedSession, decodedToken);

        CurrentTime currentTime = currentTimePort.currentTime();
        if (!storedSession.isActiveAt(currentTime)) {
            throw RefreshSessionInactiveException.inactive(
                    storedSession.sessionId(),
                    storedSession.status()
            );
        }

        TokenId replacementTokenId = tokenIdGeneratorPort.generate();
        RefreshSession rotatedSession = rotateOrRevokeOnReplay(
                storedSession,
                decodedToken.tokenId(),
                replacementTokenId,
                currentTime
        );
        IssuedTokenPair tokenPair = tokenIssuerPort.rotate(
                rotatedSession.subject(),
                rotatedSession.role(),
                rotatedSession.sessionId(),
                rotatedSession.currentTokenId(),
                rotatedSession.expiresAt(),
                currentTime
        );
        if (!rotatedSession.currentTokenId().equals(tokenPair.refreshTokenId())) {
            throw TokenCodecContractException.unexpectedTokenId(rotatedSession.currentTokenId());
        }

        refreshSessionRepositoryPort.save(rotatedSession);
        return new RotateTokensResult(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    private RefreshSession rotateOrRevokeOnReplay(
            RefreshSession storedSession,
            TokenId presentedTokenId,
            TokenId replacementTokenId,
            CurrentTime currentTime
    ) {
        try {
            return storedSession.rotate(presentedTokenId, replacementTokenId, currentTime);
        } catch (RefreshTokenReplayDetectedException exception) {
            RefreshSession revokedSession = storedSession.revoke(new RevokedAt(currentTime.value()));
            refreshSessionRepositoryPort.save(revokedSession);
            throw RefreshTokenReplayException.replayed(
                    exception.sessionId(),
                    exception.presentedTokenId()
            );
        }
    }

    private void validateContract(
            RefreshSession storedSession,
            DecodedRefreshSessionToken decodedToken
    ) {
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
