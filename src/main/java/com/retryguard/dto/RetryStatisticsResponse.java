package com.retryguard.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.OperationType;

/**
 * Aggregated retry metrics. Rates are percentages with two decimals.
 * Attempt metrics and rates only consider completed (SUCCESS or FAILED) operations.
 */
public record RetryStatisticsResponse(
        long totalOperations,
        Map<OperationStatus, Long> operationsByStatus,
        long completedOperations,
        long successfulOperations,
        long failedOperations,
        long firstAttemptSuccesses,
        long recoveredOperations,
        BigDecimal successRate,
        BigDecimal recoveryRate,
        long totalAttempts,
        BigDecimal averageAttempts,
        int maxAttempts,
        Map<Integer, Long> attemptsDistribution,
        Map<OperationType, TypeStatistics> byOperationType
) {

    public record TypeStatistics(
            long completed,
            long successful,
            long failed,
            long recovered,
            BigDecimal successRate
    ) {
    }
}
