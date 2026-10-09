package com.retryguard.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.retryguard.repository.RetryOperationRepository;

/**
 * Executions run inside the request thread, so an operation that is still RUNNING when the
 * application starts was cut off by a crash or shutdown and will never finish. Marking it FAILED
 * makes it visible and deletable again. Assumes a single application instance.
 */
@Component
public class InterruptedExecutionRecovery {

    private static final Logger log = LoggerFactory.getLogger(InterruptedExecutionRecovery.class);

    private final RetryOperationRepository repository;
    private final Clock clock;

    public InterruptedExecutionRecovery(RetryOperationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedExecutions() {
        int recovered = repository.failAllRunning(
                LocalDateTime.now(clock), "Execution interrupted by application shutdown");
        if (recovered > 0) {
            log.warn("Marked {} interrupted RUNNING operation(s) as FAILED", recovered);
        }
    }
}
