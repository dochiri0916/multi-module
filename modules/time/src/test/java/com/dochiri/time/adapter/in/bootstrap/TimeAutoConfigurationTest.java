package com.dochiri.time.adapter.in.bootstrap;

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
    void registersDefaultClockInAsiaSeoulTimezone() {
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
    void registersClockInConfiguredTimezone() {
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
    void registersTimePropertiesBean() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(TimeProperties.class);
        });
    }

    @Test
    @DisplayName("소비자가 Clock을 제공하면 기본 Clock이 물러난다")
    void backsOffWhenConsumerProvidesClock() {
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
