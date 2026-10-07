package com.retryguard.exception;

public class RetryOperationNotFoundException extends RuntimeException {

    public RetryOperationNotFoundException(Long id) {
        super("Retry operation not found with id: " + id);
    }
}
