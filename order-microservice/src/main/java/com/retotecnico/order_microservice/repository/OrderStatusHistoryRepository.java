package com.retotecnico.order_microservice.repository;

import com.retotecnico.order_microservice.domain.OrderStatusHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrderStatusHistoryRepository extends R2dbcRepository<OrderStatusHistory, UUID> {
    Flux<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
