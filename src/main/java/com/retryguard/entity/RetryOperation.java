package com.retryguard.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "retry_operations")
public class RetryOperation {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private OperationType operationType;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "initial_delay_ms", nullable = false)
    private long initialDelayMs;

    @Column(name = "failures_before_success", nullable = false)
    private int failuresBeforeSuccess;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "recovered", nullable = false)
    private boolean recovered;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RetryOperation() {
        // required by JPA
    }

    public RetryOperation(String operationName,
                          OperationType operationType,
                          int maxRetries,
                          long initialDelayMs,
                          int failuresBeforeSuccess,
                          LocalDateTime createdAt) {
        this.operationName = operationName;
        this.operationType = operationType;
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.failuresBeforeSuccess = failuresBeforeSuccess;
        this.createdAt = createdAt;
        this.status = OperationStatus.PENDING;
        this.totalAttempts = 0;
        this.recovered = false;
    }

    public void markRunning(LocalDateTime startedAt) {
        this.status = OperationStatus.RUNNING;
        this.startedAt = startedAt;
        this.completedAt = null;
        this.totalAttempts = 0;
        this.lastError = null;
        this.recovered = false;
    }

    public void markSucceeded(int attempts, String lastError, LocalDateTime completedAt) {
        this.status = OperationStatus.SUCCESS;
        this.totalAttempts = attempts;
        this.lastError = truncate(lastError);
        this.recovered = attempts > 1;
        this.completedAt = completedAt;
    }

    public void markFailed(int attempts, String lastError, LocalDateTime completedAt) {
        this.status = OperationStatus.FAILED;
        this.totalAttempts = attempts;
        this.lastError = truncate(lastError);
        this.recovered = false;
        this.completedAt = completedAt;
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public int getFailuresBeforeSuccess() {
        return failuresBeforeSuccess;
    }

    public void setFailuresBeforeSuccess(int failuresBeforeSuccess) {
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isRecovered() {
        return recovered;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
