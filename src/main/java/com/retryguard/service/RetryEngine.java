package com.retryguard.service;

import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.retryguard.exception.TransientOperationException;

/**
 * Runs an action up to {@code maxAttempts} times. Only {@link TransientOperationException}
 * is retried; any other exception is treated as permanent and stops immediately.
 * Knows nothing about the database.
 */
@Component
public class RetryEngine {

    private static final Logger log = LoggerFactory.getLogger(RetryEngine.class);

    public RetryOutcome run(int maxAttempts, IntConsumer action) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1 but was " + maxAttempts);
        }

        String lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.accept(attempt);
                log.info("Attempt {}/{} succeeded", attempt, maxAttempts);
                return new RetryOutcome(true, attempt, lastError);
            } catch (TransientOperationException ex) {
                lastError = ex.getMessage();
                log.warn("Attempt {}/{} failed (transient): {}", attempt, maxAttempts, lastError);
            } catch (RuntimeException ex) {
                lastError = describe(ex);
                log.warn("Attempt {}/{} failed (permanent, not retrying): {}", attempt, maxAttempts, lastError);
                return new RetryOutcome(false, attempt, lastError);
            }
        }

        log.warn("All {} attempts failed", maxAttempts);
        return new RetryOutcome(false, maxAttempts, lastError);
    }

    private String describe(RuntimeException ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
