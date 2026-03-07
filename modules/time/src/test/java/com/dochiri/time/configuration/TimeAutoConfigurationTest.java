package com.dochiri.time.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TimeAutoConfiguration.class));

    @Test
    void 기본_설정으로_Clock_빈이_Asia_Seoul_타임존으로_등록된다() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            Clock clock = context.getBean(Clock.class);
            assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        });
    }

    @Test
    void timezone_프로퍼티를_지정하면_해당_타임존의_Clock이_등록된다() {
        contextRunner
                .withPropertyValues("time.timezone=UTC")
                .run(context -> {
                    Clock clock = context.getBean(Clock.class);
                    assertThat(clock.getZone()).isEqualTo(ZoneId.of("UTC"));
                });
    }

    @Test
    void TimeProperties_빈이_등록된다() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TimeProperties.class);
        });
    }
}
