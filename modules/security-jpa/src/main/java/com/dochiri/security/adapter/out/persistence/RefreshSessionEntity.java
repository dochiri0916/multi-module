package com.dochiri.security.adapter.out.persistence;

import com.dochiri.jpa.entity.BaseEntity;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RevokedAt;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_sessions_session_id", columnNames = "session_id"),
                @UniqueConstraint(
                        name = "uk_refresh_sessions_current_token_id",
                        columnNames = "current_token_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_refresh_sessions_subject_revoked_expires",
                        columnList = "subject_id, revoked_at, expires_at"
                ),
                @Index(name = "idx_refresh_sessions_expires_id", columnList = "expires_at, id"),
                @Index(name = "idx_refresh_sessions_revoked_id", columnList = "revoked_at, id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshSessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter(AccessLevel.NONE)
    private Long id;

    @Version
    @Getter(AccessLevel.NONE)
    private Long version;

    @Column(name = "session_id", nullable = false, updatable = false, length = 64)
    private String sessionId;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 255)
    private String subjectId;

    @Column(name = "role_name", nullable = false, updatable = false, length = 100)
    private String roleName;

    @Column(name = "current_token_id", nullable = false, length = 64)
    private String currentTokenId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    static RefreshSessionEntity from(RefreshSession refreshSession) {
        RefreshSessionEntity entity = new RefreshSessionEntity();
        entity.sessionId = refreshSession.sessionId().value();
        entity.subjectId = refreshSession.subject().value();
        entity.roleName = refreshSession.role().value();
        entity.currentTokenId = refreshSession.currentTokenId().value();
        entity.expiresAt = refreshSession.expiresAt().value();
        entity.revokedAt = revokedAtOf(refreshSession);
        return entity;
    }

    void apply(RefreshSession refreshSession) {
        currentTokenId = refreshSession.currentTokenId().value();
        revokedAt = revokedAtOf(refreshSession);
    }

    private static Instant revokedAtOf(RefreshSession refreshSession) {
        RevokedAt revocation = refreshSession.revokedAt();
        return revocation == null ? null : revocation.value();
    }
}
