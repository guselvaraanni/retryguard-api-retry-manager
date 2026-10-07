package com.retryguard.service;

import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.retryguard.dto.RetryStatisticsResponse;
import com.retryguard.dto.RetryStatisticsResponse.TypeStatistics;
import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.OperationType;
import com.retryguard.entity.RetryOperation;
import com.retryguard.repository.RetryOperationRepository;

@Service
public class RetryStatisticsService {

    private final RetryOperationRepository repository;

    public RetryStatisticsService(RetryOperationRepository repository) {
        this.repository = repository;
    }

    public RetryStatisticsResponse getStatistics() {
        List<RetryOperation> operations = repository.findAll();

        List<RetryOperation> completed = operations.stream()
                .filter(RetryStatisticsService::isCompleted)
                .toList();

        Map<OperationStatus, Long> byStatus = countByStatus(operations);
        long successful = byStatus.get(OperationStatus.SUCCESS);
        long failed = byStatus.get(OperationStatus.FAILED);

        long recovered = completed.stream()
                .filter(RetryOperation::isRecovered)
                .count();

        long firstAttemptSuccesses = completed.stream()
                .filter(op -> op.getStatus() == OperationStatus.SUCCESS && op.getTotalAttempts() == 1)
                .count();

        long totalAttempts = completed.stream()
                .mapToInt(RetryOperation::getTotalAttempts)
                .sum();

        double averageAttempts = completed.stream()
                .collect(averagingInt(RetryOperation::getTotalAttempts));

        int maxAttempts = completed.stream()
                .mapToInt(RetryOperation::getTotalAttempts)
                .max()
                .orElse(0);

        Map<Integer, Long> attemptsDistribution = completed.stream()
                .collect(groupingBy(RetryOperation::getTotalAttempts, TreeMap::new, counting()));

        Map<OperationType, TypeStatistics> byType = completed.stream()
                .collect(groupingBy(
                        RetryOperation::getOperationType,
                        () -> new EnumMap<>(OperationType.class),
                        collectingAndThen(toList(), RetryStatisticsService::toTypeStatistics)));

        return new RetryStatisticsResponse(
                operations.size(),
                byStatus,
                completed.size(),
                successful,
                failed,
                firstAttemptSuccesses,
                recovered,
                percentage(successful, completed.size()),
                // Of the operations that hit at least one failure, how many still succeeded.
                percentage(recovered, recovered + failed),
                totalAttempts,
                BigDecimal.valueOf(averageAttempts).setScale(2, RoundingMode.HALF_UP),
                maxAttempts,
                attemptsDistribution,
                byType);
    }

    private static Map<OperationStatus, Long> countByStatus(List<RetryOperation> operations) {
        Map<OperationStatus, Long> counts = operations.stream()
                .collect(groupingBy(RetryOperation::getStatus, () -> new EnumMap<>(OperationStatus.class), counting()));
        for (OperationStatus status : OperationStatus.values()) {
            counts.putIfAbsent(status, 0L);
        }
        return counts;
    }

    private static TypeStatistics toTypeStatistics(List<RetryOperation> operations) {
        long successful = operations.stream()
                .filter(op -> op.getStatus() == OperationStatus.SUCCESS)
                .count();
        long recovered = operations.stream()
                .filter(RetryOperation::isRecovered)
                .count();
        long failed = operations.size() - successful;
        return new TypeStatistics(operations.size(), successful, failed, recovered,
                percentage(successful, operations.size()));
    }

    private static boolean isCompleted(RetryOperation operation) {
        return operation.getStatus() == OperationStatus.SUCCESS
                || operation.getStatus() == OperationStatus.FAILED;
    }

    private static BigDecimal percentage(long part, long whole) {
        if (whole == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part * 100)
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}
