package com.dochiri.time.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TimeAutoConfiguration.class));

    @Test
    @DisplayName("기본 Clock을 Asia Seoul 시간대로 등록한다")
    void 기본_설정으로_Clock_빈이_Asia_Seoul_타임존으로_등록된다() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            Clock clock = context.getBean(Clock.class);
            assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        });
    }

    @Test
    @DisplayName("설정한 시간대의 Clock을 등록한다")
    void timezone_프로퍼티를_지정하면_해당_타임존의_Clock이_등록된다() {
        // given
        ApplicationContextRunner runner = contextRunner.withPropertyValues("time.timezone=UTC");

        // when & then
        runner.run(context -> {
                    Clock clock = context.getBean(Clock.class);
                    assertThat(clock.getZone()).isEqualTo(ZoneId.of("UTC"));
                });
    }

    @Test
    @DisplayName("TimeProperties를 자동 등록한다")
    void TimeProperties_빈이_등록된다() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(TimeProperties.class);
        });
    }

    @Test
    @DisplayName("소비자가 Clock을 제공하면 기본 Clock이 물러난다")
    void 소비자가_Clock을_제공하면_기본_Clock이_물러난다() {
        // given
        Clock customClock = Clock.systemUTC();
        ApplicationContextRunner runner = contextRunner.withBean(Clock.class, () -> customClock);

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(Clock.class)).isSameAs(customClock);
        });
    }
}
