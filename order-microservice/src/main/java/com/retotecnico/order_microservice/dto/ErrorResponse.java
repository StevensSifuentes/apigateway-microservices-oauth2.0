package com.retotecnico.order_microservice.dto;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, int status, String message, String traceId) {}
