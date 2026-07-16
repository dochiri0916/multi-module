package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RefreshSessionStatus;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;

final class RefreshSessionMapper {

    private RefreshSessionMapper() {
    }

    static RefreshSessionEntity toEntity(RefreshSession refreshSession) {
        return RefreshSessionEntity.from(refreshSession);
    }

    static RefreshSession toDomain(RefreshSessionEntity entity) {
        RevokedAt revokedAt = entity.getRevokedAt() == null ? null : new RevokedAt(entity.getRevokedAt());
        RefreshSessionStatus status = revokedAt == null
                ? RefreshSessionStatus.ACTIVE
                : RefreshSessionStatus.REVOKED;
        return RefreshSession.reconstitute(
                new RefreshSessionId(entity.getRefreshSessionId()),
                new TokenId(entity.getCurrentTokenId()),
                new AuthenticationSubject(entity.getSubjectId()),
                new AuthenticationRole(entity.getRoleName()),
                new TokenExpiration(entity.getExpiresAt()),
                status,
                revokedAt
        );
    }

    static void updateEntity(RefreshSession refreshSession, RefreshSessionEntity entity) {
        entity.apply(refreshSession);
    }
}
