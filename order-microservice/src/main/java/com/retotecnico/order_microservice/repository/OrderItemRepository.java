package com.retotecnico.order_microservice.repository;

import com.retotecnico.order_microservice.domain.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrderItemRepository extends R2dbcRepository<OrderItem, UUID> {
    Flux<OrderItem> findByOrderId(UUID orderId);
}
