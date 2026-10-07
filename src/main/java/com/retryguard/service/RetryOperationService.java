package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.retryguard.dto.RetryOperationRequest;
import com.retryguard.dto.RetryOperationResponse;
import com.retryguard.entity.RetryOperation;
import com.retryguard.repository.RetryOperationRepository;

@Service
public class RetryOperationService {

    private final RetryOperationRepository repository;
    private final Clock clock;

    public RetryOperationService(RetryOperationRepository repository, Clock clock) {
        this.repository = repository;
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

    public Optional<RetryOperationResponse> findById(Long id) {
        return repository.findById(id).map(RetryOperationResponse::from);
    }

    public Optional<RetryOperationResponse> update(Long id, RetryOperationRequest request) {
        return repository.findById(id).map(operation -> {
            operation.setOperationName(request.operationName().trim());
            operation.setOperationType(request.operationType());
            operation.setMaxRetries(request.maxRetries());
            operation.setInitialDelayMs(request.initialDelayMs());
            operation.setFailuresBeforeSuccess(request.failuresBeforeSuccess());
            return RetryOperationResponse.from(repository.save(operation));
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
