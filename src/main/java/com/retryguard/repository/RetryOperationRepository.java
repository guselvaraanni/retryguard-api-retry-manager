package com.retryguard.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.retryguard.entity.RetryOperation;

public interface RetryOperationRepository extends JpaRepository<RetryOperation, Long> {

    /**
     * Atomically moves a PENDING operation to RUNNING. The database applies the WHERE check and the
     * update as one statement, so when several requests race only one of them gets 1 back.
     *
     * @return 1 if this caller claimed the operation, 0 if it is missing or not PENDING
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update RetryOperation o
            set o.status = com.retryguard.entity.OperationStatus.RUNNING,
                o.startedAt = :startedAt,
                o.completedAt = null,
                o.totalAttempts = 0,
                o.lastError = null,
                o.recovered = false,
                o.version = o.version + 1
            where o.id = :id
              and o.status = com.retryguard.entity.OperationStatus.PENDING
            """)
    int claimForExecution(@Param("id") Long id, @Param("startedAt") LocalDateTime startedAt);
}
