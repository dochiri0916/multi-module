package com.dochiri.security.jpa.entity;

import com.dochiri.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_id", columnNames = "token_id"),
        indexes = {
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false, length = 64)
    private String tokenId;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    public static RefreshToken issue(Long userId, String tokenId, Instant expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.userId = requireNonNull(userId);
        refreshToken.tokenId = requireNonNull(tokenId);
        refreshToken.expiresAt = requireNonNull(expiresAt);
        return refreshToken;
    }

    public boolean revoke(Instant now) {
        requireNonNull(now);

        if (this.revokedAt != null) {
            return false;
        }

        this.revokedAt = now;
        return true;
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        requireNonNull(now);
        return !this.expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

}
