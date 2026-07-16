package com.dochiri.errorhandling.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorContractTest {

    @Test
    @DisplayName("전역 오류 enum은 HTTP 상태와 사용자 메시지를 소유하지 않는다")
    void globalErrorEnumDoesNotOwnHttpStatusOrUserMessage() {
        // given
        Field[] declaredFields = GlobalErrorCode.class.getDeclaredFields();

        // when
        var contractFieldTypes = Arrays.stream(declaredFields)
                .filter(field -> !field.isEnumConstant())
                .filter(field -> !field.isSynthetic())
                .map(Field::getType)
                .toList();

        // then
        assertThat(contractFieldTypes).isEmpty();
    }

    @Test
    @DisplayName("전역 HTTP 매핑과 사용자 메시지는 서로 다른 provider가 소유한다")
    void separatesGlobalHttpMappingAndUserMessageProviders() {
        // given
        ApiErrorCode code = ApiErrorCode.from(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        GlobalErrorCodeMappingProvider mappingProvider = new GlobalErrorCodeMappingProvider();
        GlobalErrorMessageProvider messageProvider = new GlobalErrorMessageProvider();

        // when
        ApiErrorMapping mapping = mappingProvider.errorCodeMappings().get(code);
        ApiErrorMessage message = messageProvider.errorMessages().get(code);

        // then
        assertThat(mapping.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mapping.problemType()).isEqualTo(ProblemType.INTERNAL_ERROR);
        assertThat(message.title()).isEqualTo("서버 오류");
        assertThat(message.detail()).isEqualTo("일시적인 오류가 발생했습니다.");
    }
}
