package com.retotecnico.order_microservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String userName,
        Integer statusId,
        LocalDateTime orderDate,
        BigDecimal total,
        String traceId
) {
}
