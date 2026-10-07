package com.retryguard.exception;

import com.retryguard.entity.OperationStatus;

public class InvalidOperationStateException extends RuntimeException {

    public InvalidOperationStateException(Long id, OperationStatus currentStatus, String action) {
        super("Retry operation " + id + " cannot be " + action + " because its status is " + currentStatus);
    }
}
