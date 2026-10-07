package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.retryguard.dto.ExecutionResultResponse;
import com.retryguard.entity.AttemptStatus;
import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.RetryAttempt;
import com.retryguard.entity.RetryOperation;
import com.retryguard.exception.InvalidOperationStateException;
import com.retryguard.repository.RetryAttemptRepository;
import com.retryguard.repository.RetryOperationRepository;

@Service
public class RetryExecutionService {

    private final RetryOperationService operationService;
    private final RetryOperationRepository operationRepository;
    private final RetryAttemptRepository attemptRepository;
    private final RetryEngine retryEngine;
    private final OperationSimulator simulator;
    private final Clock clock;

    public RetryExecutionService(RetryOperationService operationService,
                                 RetryOperationRepository operationRepository,
                                 RetryAttemptRepository attemptRepository,
                                 RetryEngine retryEngine,
                                 OperationSimulator simulator,
                                 Clock clock) {
        this.operationService = operationService;
        this.operationRepository = operationRepository;
        this.attemptRepository = attemptRepository;
        this.retryEngine = retryEngine;
        this.simulator = simulator;
        this.clock = clock;
    }

    public ExecutionResultResponse execute(Long id) {
        RetryOperation operation = operationService.findOperationOrThrow(id);
        if (operation.getStatus() != OperationStatus.PENDING) {
            throw new InvalidOperationStateException(id, operation.getStatus(), "executed");
        }

        operation.markRunning(LocalDateTime.now(clock));
        RetryOperation running = operationRepository.save(operation);

        RetryOutcome outcome = retryEngine.run(
                running.getMaxRetries(),
                running.getInitialDelayMs(),
                attempt -> simulator.call(running, attempt));

        attemptRepository.saveAll(toEntities(running, outcome.attempts()));

        LocalDateTime completedAt = LocalDateTime.now(clock);
        if (outcome.succeeded()) {
            running.markSucceeded(outcome.attemptCount(), outcome.lastError(), completedAt);
        } else {
            running.markFailed(outcome.attemptCount(), outcome.lastError(), completedAt);
        }
        return ExecutionResultResponse.from(operationRepository.save(running));
    }

    private List<RetryAttempt> toEntities(RetryOperation operation, List<AttemptRecord> records) {
        return records.stream()
                .map(record -> new RetryAttempt(
                        operation,
                        record.attemptNumber(),
                        record.succeeded() ? AttemptStatus.SUCCESS : AttemptStatus.FAILED,
                        record.errorMessage(),
                        record.startedAt(),
                        record.completedAt()))
                .toList();
    }
}
