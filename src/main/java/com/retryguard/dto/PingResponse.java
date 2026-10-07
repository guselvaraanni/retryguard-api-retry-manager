package com.retryguard.dto;

import java.time.LocalDateTime;

public record PingResponse(String status, String service, LocalDateTime timestamp) {
}
