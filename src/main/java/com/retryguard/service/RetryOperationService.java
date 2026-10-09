package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retryguard.dto.RetryAttemptResponse;
import com.retryguard.dto.RetryOperationRequest;
import com.retryguard.dto.RetryOperationResponse;
import com.retryguard.entity.OperationStatus;
import com.retryguard.entity.RetryOperation;
import com.retryguard.exception.InvalidOperationStateException;
import com.retryguard.exception.RetryOperationNotFoundException;
import com.retryguard.repository.RetryAttemptRepository;
import com.retryguard.repository.RetryOperationRepository;

@Service
public class RetryOperationService {

    private final RetryOperationRepository repository;
    private final RetryAttemptRepository attemptRepository;
    private final Clock clock;

    public RetryOperationService(RetryOperationRepository repository,
                                 RetryAttemptRepository attemptRepository,
                                 Clock clock) {
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    public RetryOperationResponse create(RetryOperationRequest request) {
        RetryOperation operation = new RetryOperation(
                request.operationName().trim(),
                request.operationType(),
                request.maxRetries(),
                request.initialDelayMs(),
                request.failuresBeforeSuccess(),
                LocalDateTime.now(clock));
        return RetryOperationResponse.from(repository.save(operation));
    }

    public List<RetryOperationResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream()
                .map(RetryOperationResponse::from)
                .toList();
    }

    public RetryOperationResponse findById(Long id) {
        return RetryOperationResponse.from(findOperationOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RetryAttemptResponse> findAttempts(Long id) {
        if (!repository.existsById(id)) {
            throw new RetryOperationNotFoundException(id);
        }
        return attemptRepository.findByOperationId(id).stream()
                .map(RetryAttemptResponse::from)
                .toList();
    }

    @Transactional
    public RetryOperationResponse update(Long id, RetryOperationRequest request) {
        RetryOperation operation = findOperationOrThrow(id);
        if (operation.getStatus() != OperationStatus.PENDING) {
            throw new InvalidOperationStateException(id, operation.getStatus(), "updated");
        }
        // The entity is managed inside this transaction: Hibernate's dirty checking
        // writes these changes on commit, so no explicit save() is needed.
        operation.setOperationName(request.operationName().trim());
        operation.setOperationType(request.operationType());
        operation.setMaxRetries(request.maxRetries());
        operation.setInitialDelayMs(request.initialDelayMs());
        operation.setFailuresBeforeSuccess(request.failuresBeforeSuccess());
        return RetryOperationResponse.from(operation);
    }

    @Transactional
    public void delete(Long id) {
        RetryOperation operation = findOperationOrThrow(id);
        if (operation.getStatus() == OperationStatus.RUNNING) {
            throw new InvalidOperationStateException(id, operation.getStatus(), "deleted");
        }
        repository.delete(operation);
    }

    public RetryOperation findOperationOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RetryOperationNotFoundException(id));
    }
}
