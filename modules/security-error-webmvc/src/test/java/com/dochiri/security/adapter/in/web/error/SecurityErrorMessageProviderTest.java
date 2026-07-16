package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMessage;
import com.dochiri.errorhandling.global.error.ApiErrorMessageProvider;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorMessageProviderTest {

    private final ApiErrorMessageProvider provider = new SecurityErrorMessageProvider();

    @Test
    @DisplayName("모든 보안 오류 코드에 한국어 사용자 메시지를 제공한다")
    void providesKoreanMessageForEverySecurityErrorCode() {
        // given
        Map<ApiErrorCode, ApiErrorMessage> messages = provider.errorMessages();

        // when
        ApiErrorMessage authenticationMessage = messages.get(
                ApiErrorCode.from(SecurityErrorCode.AUTHENTICATION_REQUIRED)
        );
        ApiErrorMessage tokenMessage = messages.get(
                ApiErrorCode.from(SecurityApplicationErrorCode.TOKEN_MALFORMED)
        );

        // then
        assertThat(messages).hasSize(SecurityErrorCode.values().length + SecurityApplicationErrorCode.values().length);
        assertThat(authenticationMessage.title()).isEqualTo("인증 필요");
        assertThat(authenticationMessage.detail()).isEqualTo("인증이 필요합니다.");
        assertThat(tokenMessage.title()).isEqualTo("토큰 검증 실패");
        assertThat(tokenMessage.detail()).isEqualTo("유효하지 않은 인증 토큰입니다.");
        assertThat(messages.values())
                .allSatisfy(message -> assertThat(message.title()).isNotBlank().isNotEqualTo("internal"));
    }
}
