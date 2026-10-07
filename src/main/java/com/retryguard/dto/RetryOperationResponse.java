package com.retryguard.dto;

import java.time.LocalDateTime;

import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.OperationType;
import com.retryguard.entity.RetryOperation;

public record RetryOperationResponse(
        Long id,
        String operationName,
        OperationType operationType,
        int maxRetries,
        long initialDelayMs,
        int failuresBeforeSuccess,
        OperationStatus status,
        int totalAttempts,
        String lastError,
        boolean recovered,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static RetryOperationResponse from(RetryOperation operation) {
        return new RetryOperationResponse(
                operation.getId(),
                operation.getOperationName(),
                operation.getOperationType(),
                operation.getMaxRetries(),
                operation.getInitialDelayMs(),
                operation.getFailuresBeforeSuccess(),
                operation.getStatus(),
                operation.getTotalAttempts(),
                operation.getLastError(),
                operation.isRecovered(),
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt());
    }
}
