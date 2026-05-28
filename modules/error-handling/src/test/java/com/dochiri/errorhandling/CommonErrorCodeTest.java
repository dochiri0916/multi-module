package com.dochiri.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CommonErrorCodeTest {

    @Test
    void 공통_에러_코드는_http_status와_message를_제공한다() {
        assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()).isEqualTo("일시적인 오류가 발생했습니다.");
        assertThat(CommonErrorCode.VALIDATION_ERROR.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
