package com.dochiri.errorhandling.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class));

    @Test
    @DisplayName("소비자가 공식 MVC 예외 처리기를 제공하면 공통 fallback이 물러난다")
    void backsOffForConsumerResponseEntityExceptionHandler() {
        // given
        ConsumerExceptionHandler consumerHandler = new ConsumerExceptionHandler();
        WebApplicationContextRunner runner = contextRunner.withBean(
                "consumerExceptionHandler",
                ResponseEntityExceptionHandler.class,
                () -> consumerHandler
        );

        // when & then
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
            assertThat(context).hasSingleBean(ResponseEntityExceptionHandler.class);
            assertThat(context.getBean(ResponseEntityExceptionHandler.class)).isSameAs(consumerHandler);
        });
    }

    static final class ConsumerExceptionHandler extends ResponseEntityExceptionHandler {
    }
}
