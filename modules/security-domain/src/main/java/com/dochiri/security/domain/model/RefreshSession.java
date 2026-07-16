package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;
import com.dochiri.security.domain.exception.InvalidAuthenticationSubjectException;
import com.dochiri.security.domain.exception.InvalidCurrentTimeException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionIdException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionStateException;
import com.dochiri.security.domain.exception.InvalidTokenExpirationException;
import com.dochiri.security.domain.exception.InvalidTokenIdException;
import com.dochiri.security.domain.exception.RefreshTokenReplayDetectedException;

public record RefreshSession(
        RefreshSessionId sessionId,
        TokenId currentTokenId,
        AuthenticationSubject subject,
        AuthenticationRole role,
        TokenExpiration expiresAt,
        RefreshSessionStatus status,
        RevokedAt revokedAt
) {

    public RefreshSession {
        if (sessionId == null) {
            throw InvalidRefreshSessionIdException.required();
        }
        if (currentTokenId == null) {
            throw InvalidTokenIdException.required();
        }
        if (subject == null) {
            throw InvalidAuthenticationSubjectException.required();
        }
        if (role == null) {
            throw InvalidAuthenticationRoleException.required();
        }
        if (expiresAt == null) {
            throw InvalidTokenExpirationException.required();
        }
        if (status == null) {
            throw InvalidRefreshSessionStateException.statusRequired();
        }
        if (status == RefreshSessionStatus.REVOKED && revokedAt == null) {
            throw InvalidRefreshSessionStateException.revokedAtRequired(status);
        }
        if (status == RefreshSessionStatus.ACTIVE && revokedAt != null) {
            throw InvalidRefreshSessionStateException.activeSessionHasRevokedAt(status);
        }
    }

    public static RefreshSession issue(
            RefreshSessionId sessionId,
            TokenId currentTokenId,
            AuthenticationSubject subject,
            AuthenticationRole role,
            TokenExpiration expiresAt
    ) {
        return new RefreshSession(
                sessionId,
                currentTokenId,
                subject,
                role,
                expiresAt,
                RefreshSessionStatus.ACTIVE,
                null
        );
    }

    public static RefreshSession reconstitute(
            RefreshSessionId sessionId,
            TokenId currentTokenId,
            AuthenticationSubject subject,
            AuthenticationRole role,
            TokenExpiration expiresAt,
            RefreshSessionStatus status,
            RevokedAt revokedAt
    ) {
        return new RefreshSession(sessionId, currentTokenId, subject, role, expiresAt, status, revokedAt);
    }

    public RefreshSession rotate(
            TokenId presentedTokenId,
            TokenId replacementTokenId,
            CurrentTime currentTime
    ) {
        if (presentedTokenId == null || replacementTokenId == null) {
            throw InvalidTokenIdException.required();
        }
        if (currentTime == null) {
            throw InvalidCurrentTimeException.required();
        }
        if (!isActiveAt(currentTime)) {
            throw InvalidRefreshSessionStateException.inactive(sessionId, status);
        }
        if (!currentTokenId.equals(presentedTokenId)) {
            throw RefreshTokenReplayDetectedException.detected(
                    sessionId,
                    presentedTokenId,
                    currentTokenId
            );
        }
        if (currentTokenId.equals(replacementTokenId)) {
            throw InvalidRefreshSessionStateException.unchangedTokenId(sessionId);
        }
        return new RefreshSession(
                sessionId,
                replacementTokenId,
                subject,
                role,
                expiresAt,
                status,
                revokedAt
        );
    }

    public RefreshSession revoke(RevokedAt revocation) {
        if (status.isRevoked()) {
            return this;
        }
        if (revocation == null) {
            throw InvalidRefreshSessionStateException.revokedAtRequired(RefreshSessionStatus.REVOKED);
        }
        return new RefreshSession(
                sessionId,
                currentTokenId,
                subject,
                role,
                expiresAt,
                RefreshSessionStatus.REVOKED,
                revocation
        );
    }

    public boolean isActiveAt(CurrentTime currentTime) {
        if (currentTime == null) {
            throw InvalidCurrentTimeException.required();
        }
        return status == RefreshSessionStatus.ACTIVE && !expiresAt.isExpiredAt(currentTime);
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof RefreshSession that
                && sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
