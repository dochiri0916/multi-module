package com.dochiri.security.application.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidTokenExceptionTest {

    @Test
    @DisplayName("토큰 검증 실패 정적 팩토리는 원인별 Application 오류 코드를 보존한다")
    void preservesErrorCodeForEachInvalidTokenReason() {
        // given
        List<SecurityApplicationErrorCode> expectedCodes = List.of(
                SecurityApplicationErrorCode.TOKEN_EXPIRED,
                SecurityApplicationErrorCode.TOKEN_MALFORMED,
                SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID,
                SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING,
                SecurityApplicationErrorCode.TOKEN_ROLE_MISSING,
                SecurityApplicationErrorCode.TOKEN_ID_MISSING,
                SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING
        );

        // when
        List<SecurityApplicationErrorCode> actualCodes = List.of(
                        InvalidTokenException.expired(),
                        InvalidTokenException.malformed(),
                        InvalidTokenException.invalidCategory(),
                        InvalidTokenException.missingSubject(),
                        InvalidTokenException.missingRole(),
                        InvalidTokenException.missingTokenId(),
                        InvalidTokenException.missingExpiration()
                ).stream()
                .map(InvalidTokenException::code)
                .toList();

        // then
        assertThat(actualCodes).containsExactlyElementsOf(expectedCodes);
    }
}
