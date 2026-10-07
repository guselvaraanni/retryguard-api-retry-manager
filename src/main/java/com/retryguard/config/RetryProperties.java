package com.retryguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "retryguard.retry")
public record RetryProperties(

        @Min(1)
        @Max(60_000)
        long maxDelayMs
) {
}
