package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationSubjectException;
import com.dochiri.security.domain.exception.SecurityDomainErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationSubjectTest {

    @Test
    @DisplayName("인증 주체의 앞뒤 공백을 제거한다")
    void trimsSurroundingAuthenticationSubjectWhitespace() {
        // given
        String value = "  member-01  ";

        // when
        AuthenticationSubject subject = new AuthenticationSubject(value);

        // then
        assertThat(subject.value()).isEqualTo("member-01");
    }

    @Test
    @DisplayName("인증 주체가 null이면 전용 오류 코드로 거부한다")
    void rejectsNullAuthenticationSubjectWithDedicatedErrorCode() {
        // given
        String value = missingValue();

        // when & then
        assertThatThrownBy(() -> new AuthenticationSubject(value))
                .isInstanceOfSatisfying(InvalidAuthenticationSubjectException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_SUBJECT_REQUIRED));
    }

    @Test
    @DisplayName("인증 주체가 blank이면 전용 오류 코드로 거부한다")
    void rejectsBlankAuthenticationSubjectWithDedicatedErrorCode() {
        // given
        String value = "   ";

        // when & then
        assertThatThrownBy(() -> new AuthenticationSubject(value))
                .isInstanceOfSatisfying(InvalidAuthenticationSubjectException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_SUBJECT_BLANK));
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
