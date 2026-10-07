package com.retryguard.entity;

import java.time.LocalDateTime;

import com.retryguard.util.TextUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "retry_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_retry_attempts_operation_attempt",
                columnNames = {"operation_id", "attempt_number"}))
public class RetryAttempt {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private RetryOperation retryOperation;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "error_message", length = MAX_ERROR_LENGTH)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected RetryAttempt() {
        // required by JPA
    }

    public RetryAttempt(RetryOperation retryOperation,
                        int attemptNumber,
                        AttemptStatus status,
                        String errorMessage,
                        LocalDateTime startedAt,
                        LocalDateTime completedAt) {
        this.retryOperation = retryOperation;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.errorMessage = TextUtils.truncate(errorMessage, MAX_ERROR_LENGTH);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public RetryOperation getRetryOperation() {
        return retryOperation;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
