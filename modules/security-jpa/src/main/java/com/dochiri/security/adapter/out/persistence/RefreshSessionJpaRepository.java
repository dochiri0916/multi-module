package com.dochiri.security.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, Long> {

    Optional<RefreshSessionEntity> findByRefreshSessionId(String refreshSessionId);

    Optional<RefreshSessionEntity> findByCurrentTokenId(String currentTokenId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSessionEntity session where session.refreshSessionId = :refreshSessionId")
    Optional<RefreshSessionEntity> findByRefreshSessionIdForUpdate(
            @Param("refreshSessionId") String refreshSessionId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSessionEntity session
               set session.revokedAt = :revokedAt
             where session.subjectId = :subjectId
               and session.revokedAt is null
               and session.expiresAt > :revokedAt
            """)
    int revokeAllActiveBySubject(
            @Param("subjectId") String subjectId,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            delete from refresh_sessions
             where expires_at < :expiredBefore
                or (revoked_at is not null and revoked_at < :revokedBefore)
             order by id
             limit :batchSize
            """, nativeQuery = true)
    int deleteCleanupBatch(
            @Param("expiredBefore") Instant expiredBefore,
            @Param("revokedBefore") Instant revokedBefore,
            @Param("batchSize") int batchSize
    );
}
