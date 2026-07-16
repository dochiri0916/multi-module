package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupBatchSizeException;
import com.dochiri.security.application.exception.InvalidRefreshSessionCleanupCountException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsCommand;
import com.dochiri.security.application.port.in.CleanupRefreshSessionsResult;
import com.dochiri.security.application.port.out.RefreshSessionCleanupPort;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RevokedAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CleanupRefreshSessionsServiceTest {

    private static final CurrentTime EXPIRED_BEFORE =
            new CurrentTime(Instant.parse("2029-01-01T00:00:00Z"));
    private static final RevokedAt REVOKED_BEFORE =
            new RevokedAt(Instant.parse("2028-12-01T00:00:00Z"));

    @Test
    @DisplayName("cleanup은 소비자가 지정한 보관 경계와 batch 크기로 세션을 한 번만 삭제한다")
    void deletesSessionsOnceWithConsumerRetentionBoundsAndBatchSize() {
        // given
        RecordingCleanupPort cleanupPort = new RecordingCleanupPort();
        cleanupPort.deletedCount(100);
        CleanupRefreshSessionsService service = new CleanupRefreshSessionsService(cleanupPort);
        CleanupRefreshSessionsCommand command = new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                100
        );

        // when
        CleanupRefreshSessionsResult result = service.execute(command);

        // then
        assertThat(result.deletedCount()).isEqualTo(100);
        assertThat(result.moreMayRemain()).isTrue();
        assertThat(cleanupPort.expiredBefore()).isEqualTo(EXPIRED_BEFORE);
        assertThat(cleanupPort.revokedBefore()).isEqualTo(REVOKED_BEFORE);
        assertThat(cleanupPort.batchSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("삭제 건수가 batch 크기보다 작으면 추가 cleanup이 필요하지 않다고 응답한다")
    void reportsNoMoreCleanupWhenDeletedCountIsBelowBatchSize() {
        // given
        RecordingCleanupPort cleanupPort = new RecordingCleanupPort();
        cleanupPort.deletedCount(99);
        CleanupRefreshSessionsService service = new CleanupRefreshSessionsService(cleanupPort);

        // when
        CleanupRefreshSessionsResult result = service.execute(new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                100
        ));

        // then
        assertThat(result.deletedCount()).isEqualTo(99);
        assertThat(result.moreMayRemain()).isFalse();
    }

    @Test
    @DisplayName("cleanup batch 크기가 허용 범위를 벗어나면 전용 Application 오류로 거부한다")
    void rejectsCleanupBatchSizeAboveAllowedRange() {
        // given
        int excessiveBatchSize = 1_001;

        // when & then
        assertThatThrownBy(() -> new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                excessiveBatchSize
        )).isInstanceOfSatisfying(InvalidRefreshSessionCleanupBatchSizeException.class, exception -> {
            assertThat(exception.code())
                    .isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID);
            assertThat(exception.batchSize()).isEqualTo(excessiveBatchSize);
        });
    }

    @Test
    @DisplayName("cleanup batch 크기가 0이면 전용 Application 오류로 거부한다")
    void rejectsZeroCleanupBatchSize() {
        // given
        int emptyBatchSize = 0;

        // when & then
        assertThatThrownBy(() -> new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                emptyBatchSize
        )).isInstanceOfSatisfying(InvalidRefreshSessionCleanupBatchSizeException.class, exception -> {
            assertThat(exception.code())
                    .isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_BATCH_SIZE_INVALID);
            assertThat(exception.batchSize()).isEqualTo(emptyBatchSize);
        });
    }

    @Test
    @DisplayName("cleanup Port가 음수 삭제 건수를 반환하면 계약 위반으로 거부한다")
    void rejectsNegativeDeletedCountFromCleanupPort() {
        // given
        RecordingCleanupPort cleanupPort = new RecordingCleanupPort();
        cleanupPort.deletedCount(-1);
        CleanupRefreshSessionsService service = new CleanupRefreshSessionsService(cleanupPort);

        // when & then
        assertThatThrownBy(() -> service.execute(new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                100
        ))).isInstanceOfSatisfying(InvalidRefreshSessionCleanupCountException.class, exception -> {
            assertThat(exception.code())
                    .isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID);
            assertThat(exception.deletedCount()).isEqualTo(-1);
        });
    }

    @Test
    @DisplayName("cleanup Port가 요청 batch보다 큰 삭제 건수를 반환하면 계약 위반으로 거부한다")
    void rejectsDeletedCountAboveRequestedBatchFromCleanupPort() {
        // given
        RecordingCleanupPort cleanupPort = new RecordingCleanupPort();
        cleanupPort.deletedCount(101);
        CleanupRefreshSessionsService service = new CleanupRefreshSessionsService(cleanupPort);

        // when & then
        assertThatThrownBy(() -> service.execute(new CleanupRefreshSessionsCommand(
                EXPIRED_BEFORE,
                REVOKED_BEFORE,
                100
        ))).isInstanceOfSatisfying(InvalidRefreshSessionCleanupCountException.class, exception -> {
            assertThat(exception.code())
                    .isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_CLEANUP_COUNT_INVALID);
            assertThat(exception.deletedCount()).isEqualTo(101);
        });
    }

    private static final class RecordingCleanupPort implements RefreshSessionCleanupPort {

        private int result;
        private CurrentTime capturedExpiredBefore;
        private RevokedAt capturedRevokedBefore;
        private int capturedBatchSize;

        @Override
        public int deleteBatch(CurrentTime expiredBefore, RevokedAt revokedBefore, int batchSize) {
            capturedExpiredBefore = expiredBefore;
            capturedRevokedBefore = revokedBefore;
            capturedBatchSize = batchSize;
            return result;
        }

        void deletedCount(int value) {
            result = value;
        }

        CurrentTime expiredBefore() {
            return capturedExpiredBefore;
        }

        RevokedAt revokedBefore() {
            return capturedRevokedBefore;
        }

        int batchSize() {
            return capturedBatchSize;
        }
    }
}
