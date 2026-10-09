package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.retryguard.dto.ExecutionResultResponse;
import com.retryguard.entity.AttemptStatus;
import com.retryguard.entity.RetryAttempt;
import com.retryguard.entity.RetryOperation;
import com.retryguard.exception.InvalidOperationStateException;
import com.retryguard.repository.RetryAttemptRepository;
import com.retryguard.repository.RetryOperationRepository;

/**
 * Transaction boundaries are deliberately split:
 * 1. claim (short transaction, commits RUNNING so other requests see it),
 * 2. retry loop (no transaction: it sleeps, and must not hold a DB connection),
 * 3. record the outcome (one transaction, so attempts and final status commit together).
 */
@Service
public class RetryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutionService.class);

    private final RetryOperationService operationService;
    private final RetryOperationRepository operationRepository;
    private final RetryAttemptRepository attemptRepository;
    private final RetryEngine retryEngine;
    private final OperationSimulator simulator;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RetryExecutionService(RetryOperationService operationService,
                                 RetryOperationRepository operationRepository,
                                 RetryAttemptRepository attemptRepository,
                                 RetryEngine retryEngine,
                                 OperationSimulator simulator,
                                 TransactionTemplate transactionTemplate,
                                 Clock clock) {
        this.operationService = operationService;
        this.operationRepository = operationRepository;
        this.attemptRepository = attemptRepository;
        this.retryEngine = retryEngine;
        this.simulator = simulator;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public ExecutionResultResponse execute(Long id) {
        int claimed = operationRepository.claimForExecution(id, LocalDateTime.now(clock));
        if (claimed == 0) {
            RetryOperation existing = operationService.findOperationOrThrow(id);
            throw new InvalidOperationStateException(id, existing.getStatus(), "executed");
        }
        RetryOperation running = operationService.findOperationOrThrow(id);
        log.info("Executing operation {} ('{}'), up to {} attempts",
                id, running.getOperationName(), running.getMaxRetries());

        RetryOutcome outcome = retryEngine.run(
                running.getMaxRetries(),
                running.getInitialDelayMs(),
                attempt -> simulator.call(running, attempt));

        ExecutionResultResponse result = transactionTemplate.execute(status -> recordOutcome(id, outcome));
        log.info("Operation {} finished: {} after {} attempt(s)", id, result.status(), result.totalAttempts());
        return result;
    }

    private ExecutionResultResponse recordOutcome(Long id, RetryOutcome outcome) {
        RetryOperation operation = operationService.findOperationOrThrow(id);
        attemptRepository.saveAll(toEntities(operation, outcome.attempts()));

        LocalDateTime completedAt = LocalDateTime.now(clock);
        if (outcome.succeeded()) {
            operation.markSucceeded(outcome.attemptCount(), outcome.lastError(), completedAt);
        } else {
            operation.markFailed(outcome.attemptCount(), outcome.lastError(), completedAt);
        }
        return ExecutionResultResponse.from(operation);
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
