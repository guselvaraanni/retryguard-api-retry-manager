package com.retryguard.service;

import java.util.List;

/**
 * Result of running an action through the retry engine.
 *
 * @param succeeded true if any attempt succeeded
 * @param lastError message of the most recent failed attempt, or null if none failed
 * @param attempts  every attempt made, in order
 */
public record RetryOutcome(boolean succeeded, String lastError, List<AttemptRecord> attempts) {

    public RetryOutcome {
        attempts = List.copyOf(attempts);
    }

    public int attemptCount() {
        return attempts.size();
    }
}
