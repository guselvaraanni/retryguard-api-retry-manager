package com.retryguard.exception;

/**
 * A temporary failure that may succeed if the same call is tried again
 * (timeout, 503, rate limit). The retry engine retries only this type.
 */
public class TransientOperationException extends RuntimeException {

    public TransientOperationException(String message) {
        super(message);
    }
}
