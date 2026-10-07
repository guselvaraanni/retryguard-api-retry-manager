package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.retryguard.dto.RetryOperationResponse;
import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.RetryOperation;
import com.retryguard.exception.InvalidOperationStateException;
import com.retryguard.repository.RetryOperationRepository;

@Service
public class RetryExecutionService {

    private final RetryOperationService operationService;
    private final RetryOperationRepository repository;
    private final RetryEngine retryEngine;
    private final OperationSimulator simulator;
    private final Clock clock;

    public RetryExecutionService(RetryOperationService operationService,
                                 RetryOperationRepository repository,
                                 RetryEngine retryEngine,
                                 OperationSimulator simulator,
                                 Clock clock) {
        this.operationService = operationService;
        this.repository = repository;
        this.retryEngine = retryEngine;
        this.simulator = simulator;
        this.clock = clock;
    }

    public RetryOperationResponse execute(Long id) {
        RetryOperation operation = operationService.findOperationOrThrow(id);
        if (operation.getStatus() != OperationStatus.PENDING) {
            throw new InvalidOperationStateException(id, operation.getStatus(), "executed");
        }

        operation.markRunning(LocalDateTime.now(clock));
        repository.save(operation);

        RetryOutcome outcome = retryEngine.run(
                operation.getMaxRetries(),
                operation.getInitialDelayMs(),
                attempt -> simulator.call(operation, attempt));

        LocalDateTime completedAt = LocalDateTime.now(clock);
        if (outcome.succeeded()) {
            operation.markSucceeded(outcome.attempts(), outcome.lastError(), completedAt);
        } else {
            operation.markFailed(outcome.attempts(), outcome.lastError(), completedAt);
        }
        return RetryOperationResponse.from(repository.save(operation));
    }
}
