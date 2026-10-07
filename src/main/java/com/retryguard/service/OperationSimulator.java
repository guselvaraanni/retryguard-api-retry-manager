package com.retryguard.service;

import org.springframework.stereotype.Component;

import com.retryguard.entity.RetryOperation;
import com.retryguard.exception.TransientOperationException;

/**
 * Stands in for an unreliable external call. Deterministic: the first
 * {@code failuresBeforeSuccess} attempts fail, every later attempt succeeds.
 */
@Component
public class OperationSimulator {

    public void call(RetryOperation operation, int attemptNumber) {
        if (attemptNumber <= operation.getFailuresBeforeSuccess()) {
            throw new TransientOperationException(
                    "Simulated transient failure on attempt " + attemptNumber
                            + " (503 Service Unavailable)");
        }
    }
}
