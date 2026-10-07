package com.retryguard.controller;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retryguard.dto.PingResponse;

@RestController
@RequestMapping("/api")
public class PingController {

    private final Clock clock;

    public PingController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("UP", "RetryGuard", LocalDateTime.now(clock));
    }
}
