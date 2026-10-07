package com.retryguard.service;

/**
 * Result of running an action through the retry engine.
 *
 * @param succeeded true if any attempt succeeded
 * @param attempts  number of attempts actually made
 * @param lastError message of the most recent failed attempt, or null if none failed
 */
public record RetryOutcome(boolean succeeded, int attempts, String lastError) {
}
