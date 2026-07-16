package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidRefreshSessionIdException;
import com.dochiri.security.domain.exception.SecurityDomainErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshSessionIdTest {

    @Test
    @DisplayName("리프레시 세션 식별자는 앞뒤 공백을 제거한다")
    void trimsSurroundingRefreshSessionIdentifierWhitespace() {
        // given
        String rawSessionId = "  session-id-01  ";

        // when
        RefreshSessionId sessionId = new RefreshSessionId(rawSessionId);

        // then
        assertThat(sessionId.value()).isEqualTo("session-id-01");
    }

    @Test
    @DisplayName("리프레시 세션 식별자가 null이면 전용 오류 코드로 거부한다")
    void rejectsNullRefreshSessionIdentifierWithDedicatedErrorCode() {
        // given
        String missingSessionId = missingValue();

        // when & then
        assertThatThrownBy(() -> new RefreshSessionId(missingSessionId))
                .isInstanceOfSatisfying(InvalidRefreshSessionIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_ID_REQUIRED)
                );
    }

    @Test
    @DisplayName("리프레시 세션 식별자가 blank이면 전용 오류 코드로 거부한다")
    void rejectsBlankRefreshSessionIdentifierWithDedicatedErrorCode() {
        // given
        String blankSessionId = "   ";

        // when & then
        assertThatThrownBy(() -> new RefreshSessionId(blankSessionId))
                .isInstanceOfSatisfying(InvalidRefreshSessionIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_ID_BLANK)
                );
    }

    @Test
    @DisplayName("리프레시 세션 식별자 generate 팩토리는 하이픈 없는 UUID 문자열을 만든다")
    void generatesHyphenlessUuidRefreshSessionIdentifier() {
        // given
        int expectedLength = 32;

        // when
        RefreshSessionId sessionId = RefreshSessionId.generate();

        // then
        assertThat(sessionId.value()).hasSize(expectedLength).doesNotContain("-");
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
