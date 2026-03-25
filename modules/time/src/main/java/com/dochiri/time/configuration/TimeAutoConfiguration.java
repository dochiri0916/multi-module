package com.dochiri.time.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@AutoConfiguration
@EnableConfigurationProperties(TimeProperties.class)
public class TimeAutoConfiguration {

    @Bean
    Clock clock(TimeProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }

}