package com.dochiri.security.application.port.in;

import com.dochiri.security.application.exception.InvalidRefreshTokenRevocationCountException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevokeAllRefreshTokensResultTest {

    @Test
    @DisplayName("음수 리프레시 토큰 폐기 건수는 전용 Application 예외로 거부한다")
    void 음수_리프레시_토큰_폐기_건수는_전용_Application_예외로_거부한다() {
        // given
        int revokedCount = -1;

        // when & then
        assertThatThrownBy(() -> new RevokeAllRefreshTokensResult(revokedCount))
                .isInstanceOfSatisfying(InvalidRefreshTokenRevocationCountException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.REFRESH_TOKEN_REVOCATION_COUNT_INVALID);
                    assertThat(exception.revokedCount()).isEqualTo(revokedCount);
                });
    }
}
