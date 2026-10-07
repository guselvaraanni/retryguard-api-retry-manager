package com.retryguard.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.RetryOperation;

public record ExecutionResultResponse(
        Long operationId,
        String operationName,
        OperationStatus status,
        int totalAttempts,
        int maxRetries,
        boolean recovered,
        String lastError,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long durationMs
) {

    public static ExecutionResultResponse from(RetryOperation operation) {
        return new ExecutionResultResponse(
                operation.getId(),
                operation.getOperationName(),
                operation.getStatus(),
                operation.getTotalAttempts(),
                operation.getMaxRetries(),
                operation.isRecovered(),
                operation.getLastError(),
                operation.getStartedAt(),
                operation.getCompletedAt(),
                Duration.between(operation.getStartedAt(), operation.getCompletedAt()).toMillis());
    }
}
