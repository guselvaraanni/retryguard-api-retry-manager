package com.retryguard.util;

/**
 * Exponential backoff: delay = initialDelayMs * 2^(failedAttempt - 1), capped at maxDelayMs.
 */
public final class BackoffCalculator {

    private static final int MAX_SHIFT = 62;

    private BackoffCalculator() {
    }

    public static long delayForAttempt(long initialDelayMs, int failedAttempt, long maxDelayMs) {
        if (initialDelayMs < 0 || maxDelayMs < 0) {
            throw new IllegalArgumentException("Delays must not be negative");
        }
        if (failedAttempt < 1) {
            throw new IllegalArgumentException("failedAttempt must be at least 1 but was " + failedAttempt);
        }

        int shift = Math.min(failedAttempt - 1, MAX_SHIFT);
        long multiplier = 1L << shift;

        // Checking before multiplying avoids long overflow for large attempt numbers.
        if (initialDelayMs > maxDelayMs / multiplier) {
            return maxDelayMs;
        }
        return Math.min(initialDelayMs * multiplier, maxDelayMs);
    }
}
