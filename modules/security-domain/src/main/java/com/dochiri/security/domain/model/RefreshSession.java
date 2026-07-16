package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;
import com.dochiri.security.domain.exception.InvalidAuthenticationSubjectException;
import com.dochiri.security.domain.exception.InvalidCurrentTimeException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionIdException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionStateException;
import com.dochiri.security.domain.exception.InvalidTokenExpirationException;
import com.dochiri.security.domain.exception.InvalidTokenIdException;
import com.dochiri.security.domain.exception.RefreshTokenReplayDetectedException;

public final class RefreshSession {

    private final RefreshSessionId sessionId;
    private final TokenId currentTokenId;
    private final AuthenticationSubject subject;
    private final AuthenticationRole role;
    private final TokenExpiration expiresAt;
    private final RefreshSessionStatus status;
    private final RevokedAt revokedAt;

    private RefreshSession(
            RefreshSessionId sessionId,
            TokenId currentTokenId,
            AuthenticationSubject subject,
            AuthenticationRole role,
            TokenExpiration expiresAt,
            RefreshSessionStatus status,
            RevokedAt revokedAt
    ) {
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
        validateState(status, revokedAt);
        this.sessionId = sessionId;
        this.currentTokenId = currentTokenId;
        this.subject = subject;
        this.role = role;
        this.expiresAt = expiresAt;
        this.status = status;
        this.revokedAt = revokedAt;
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
        if (isRevoked()) {
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

    public boolean isRevoked() {
        return status == RefreshSessionStatus.REVOKED;
    }

    public RefreshSessionId sessionId() {
        return sessionId;
    }

    public TokenId currentTokenId() {
        return currentTokenId;
    }

    public AuthenticationSubject subject() {
        return subject;
    }

    public AuthenticationRole role() {
        return role;
    }

    public TokenExpiration expiresAt() {
        return expiresAt;
    }

    public RefreshSessionStatus status() {
        return status;
    }

    public RevokedAt revokedAt() {
        return revokedAt;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RefreshSession that)) {
            return false;
        }
        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }

    private static void validateState(RefreshSessionStatus status, RevokedAt revokedAt) {
        if (status == RefreshSessionStatus.REVOKED && revokedAt == null) {
            throw InvalidRefreshSessionStateException.revokedAtRequired(status);
        }
        if (status == RefreshSessionStatus.ACTIVE && revokedAt != null) {
            throw InvalidRefreshSessionStateException.activeSessionHasRevokedAt(status);
        }
    }
}
