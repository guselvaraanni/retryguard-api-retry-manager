package com.retryguard.dto;

import com.retryguard.entity.OperationType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RetryOperationRequest(

        @NotBlank(message = "operationName is required")
        @Size(max = 100, message = "operationName must be at most 100 characters")
        String operationName,

        @NotNull(message = "operationType is required")
        OperationType operationType,

        @NotNull(message = "maxRetries is required")
        @Min(value = 1, message = "maxRetries must be at least 1")
        @Max(value = 10, message = "maxRetries must be at most 10")
        Integer maxRetries,

        @NotNull(message = "initialDelayMs is required")
        @Min(value = 1, message = "initialDelayMs must be at least 1")
        @Max(value = 5000, message = "initialDelayMs must be at most 5000")
        Long initialDelayMs,

        @NotNull(message = "failuresBeforeSuccess is required")
        @Min(value = 0, message = "failuresBeforeSuccess must be at least 0")
        @Max(value = 20, message = "failuresBeforeSuccess must be at most 20")
        Integer failuresBeforeSuccess
) {
}
