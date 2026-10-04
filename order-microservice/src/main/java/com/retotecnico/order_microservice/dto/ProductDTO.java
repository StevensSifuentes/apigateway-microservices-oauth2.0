package com.retotecnico.order_microservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(UUID id, String name, BigDecimal price, Integer stock, Boolean status) {
}
