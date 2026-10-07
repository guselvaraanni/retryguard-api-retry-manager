package com.retryguard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retryguard.entity.RetryOperation;

public interface RetryOperationRepository extends JpaRepository<RetryOperation, Long> {
}
