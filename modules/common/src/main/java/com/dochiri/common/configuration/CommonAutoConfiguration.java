package com.dochiri.common.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
public class CommonAutoConfiguration {

    @Bean
    Clock clock(CommonProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
