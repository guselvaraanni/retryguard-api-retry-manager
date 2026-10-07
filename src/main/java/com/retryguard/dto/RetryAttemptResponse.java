package com.retryguard.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import com.retryguard.entity.AttemptStatus;
import com.retryguard.entity.RetryAttempt;

public record RetryAttemptResponse(
        int attemptNumber,
        AttemptStatus status,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long durationMs
) {

    public static RetryAttemptResponse from(RetryAttempt attempt) {
        return new RetryAttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).toMillis());
    }
}
