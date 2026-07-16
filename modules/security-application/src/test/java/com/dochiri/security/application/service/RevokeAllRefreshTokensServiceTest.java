package com.dochiri.security.application.service;

import com.dochiri.security.application.port.in.RevokeAllRefreshTokensCommand;
import com.dochiri.security.application.port.in.RevokeAllRefreshTokensResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dochiri.security.application.service.SecurityApplicationTestFixture.NOW;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SUBJECT;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.fixedCurrentTime;
import static org.assertj.core.api.Assertions.assertThat;

class RevokeAllRefreshTokensServiceTest {

    @Test
    @DisplayName("인증 주체의 모든 활성 리프레시 토큰을 현재 시각으로 폐기한다")
    void 인증_주체의_모든_활성_리프레시_토큰을_현재_시각으로_폐기한다() {
        // given
        SecurityApplicationTestFixture.RecordingBulkRevocationPort bulkPort =
                new SecurityApplicationTestFixture.RecordingBulkRevocationPort();
        bulkPort.result(3);
        RevokeAllRefreshTokensService service = new RevokeAllRefreshTokensService(
                bulkPort,
                fixedCurrentTime()
        );

        // when
        RevokeAllRefreshTokensResult result = service.execute(new RevokeAllRefreshTokensCommand(SUBJECT));

        // then
        assertThat(result.revokedCount()).isEqualTo(3);
        assertThat(bulkPort.revokedSubject()).isEqualTo(SUBJECT);
        assertThat(bulkPort.revokedAt().value()).isEqualTo(NOW.value());
    }
}
