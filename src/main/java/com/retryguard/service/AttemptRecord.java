package com.retryguard.service;

import java.time.LocalDateTime;

public record AttemptRecord(
        int attemptNumber,
        boolean succeeded,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
