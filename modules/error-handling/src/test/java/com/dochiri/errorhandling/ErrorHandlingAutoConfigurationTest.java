package com.dochiri.errorhandling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class));

    @Test
    @DisplayName("Servlet Web 애플리케이션에 공통 오류 처리 계약을 자동 구성한다")
    void Servlet_Web_애플리케이션에_공통_오류_처리_계약을_자동_구성한다() {
        // given
        ApiErrorCode globalCode = ApiErrorCode.from(GlobalErrorCode.INTERNAL_SERVER_ERROR);

        // when & then
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ApiExceptionMapper.class);
            assertThat(context).hasSingleBean(ApiErrorMessageCatalog.class);
            assertThat(context).hasSingleBean(ApiProblemDetailFactory.class);
            assertThat(context).hasSingleBean(ApiErrorContractValidator.class);
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context.getBean(ApiErrorContractValidator.class).validate()).contains(globalCode);
        });
    }

    @Test
    @DisplayName("메시지가 없는 Context HTTP 매핑이 등록되면 애플리케이션 시작에 실패한다")
    void 메시지가_없는_Context_HTTP_매핑이_등록되면_애플리케이션_시작에_실패한다() {
        // given
        ErrorCodeMappingProvider incompleteProvider = () -> Map.of(
                ApiErrorCode.from(OrphanErrorCode.MISSING_MESSAGE),
                new ApiErrorMapping(HttpStatus.BAD_REQUEST, ProblemType.BAD_REQUEST)
        );

        // when & then
        contextRunner
                .withBean("incompleteProvider", ErrorCodeMappingProvider.class, () -> incompleteProvider)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .rootCause()
                            .hasMessageContaining("일치하지 않습니다")
                            .hasMessageContaining("ORPHAN.MISSING_MESSAGE");
                });
    }

    private enum OrphanErrorCode {
        MISSING_MESSAGE
    }
}
