package com.retryguard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.retryguard.entity.RetryAttempt;

public interface RetryAttemptRepository extends JpaRepository<RetryAttempt, Long> {

    @Query("""
            select a from RetryAttempt a
            where a.retryOperation.id = :operationId
            order by a.attemptNumber
            """)
    List<RetryAttempt> findByOperationId(@Param("operationId") Long operationId);
}
