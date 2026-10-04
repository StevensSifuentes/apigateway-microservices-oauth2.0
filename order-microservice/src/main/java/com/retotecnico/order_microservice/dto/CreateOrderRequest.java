package com.retotecnico.order_microservice.dto;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(List<ItemRequest> items) {
    public record ItemRequest(UUID productId, Integer quantity) {}
}