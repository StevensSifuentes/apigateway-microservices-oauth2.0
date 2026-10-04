package com.retotecnico.order_microservice.repository;

import com.retotecnico.order_microservice.domain.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrderRepository extends R2dbcRepository<Order, UUID> {
    Flux<Order> findByUserId(UUID userId);
    Mono<Order> findByIdAndUserId(UUID id, UUID userId);
}
