package com.retryguard.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.retryguard.dto.ExecutionResultResponse;
import com.retryguard.dto.RetryAttemptResponse;
import com.retryguard.dto.RetryOperationRequest;
import com.retryguard.dto.RetryOperationResponse;
import com.retryguard.dto.RetryStatisticsResponse;
import com.retryguard.service.RetryExecutionService;
import com.retryguard.service.RetryOperationService;
import com.retryguard.service.RetryStatisticsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/operations")
public class RetryOperationController {

    private final RetryOperationService operationService;
    private final RetryExecutionService executionService;
    private final RetryStatisticsService statisticsService;

    public RetryOperationController(RetryOperationService operationService,
                                    RetryExecutionService executionService,
                                    RetryStatisticsService statisticsService) {
        this.operationService = operationService;
        this.executionService = executionService;
        this.statisticsService = statisticsService;
    }

    @PostMapping
    public ResponseEntity<RetryOperationResponse> create(@Valid @RequestBody RetryOperationRequest request) {
        RetryOperationResponse created = operationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RetryOperationResponse> findAll() {
        return operationService.findAll();
    }

    @GetMapping("/statistics")
    public RetryStatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/{id}")
    public RetryOperationResponse findById(@PathVariable Long id) {
        return operationService.findById(id);
    }

    @PostMapping("/{id}/execute")
    public ExecutionResultResponse execute(@PathVariable Long id) {
        return executionService.execute(id);
    }

    @GetMapping("/{id}/attempts")
    public List<RetryAttemptResponse> findAttempts(@PathVariable Long id) {
        return operationService.findAttempts(id);
    }

    @PutMapping("/{id}")
    public RetryOperationResponse update(@PathVariable Long id,
                                         @Valid @RequestBody RetryOperationRequest request) {
        return operationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        operationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
