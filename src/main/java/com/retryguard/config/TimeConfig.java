package com.retryguard.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /**
     * Ticks in microseconds because PostgreSQL timestamps store microseconds. A finer clock would
     * make API responses show values that differ from what is later read back from the database.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemDefaultZone(), Duration.ofNanos(1_000));
    }
}
