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
import com.retryguard.service.RetryExecutionService;
import com.retryguard.service.RetryOperationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/operations")
public class RetryOperationController {

    private final RetryOperationService service;
    private final RetryExecutionService executionService;

    public RetryOperationController(RetryOperationService service, RetryExecutionService executionService) {
        this.service = service;
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<RetryOperationResponse> create(@Valid @RequestBody RetryOperationRequest request) {
        RetryOperationResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RetryOperationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RetryOperationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/execute")
    public ExecutionResultResponse execute(@PathVariable Long id) {
        return executionService.execute(id);
    }

    @GetMapping("/{id}/attempts")
    public List<RetryAttemptResponse> findAttempts(@PathVariable Long id) {
        return service.findAttempts(id);
    }

    @PutMapping("/{id}")
    public RetryOperationResponse update(@PathVariable Long id,
                                         @Valid @RequestBody RetryOperationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
