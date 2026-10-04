package com.retotecnico.order_microservice.repository;

import com.retotecnico.order_microservice.domain.OrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface OrderStatusRepository extends R2dbcRepository<OrderStatus, Integer> {
    Mono<OrderStatus> findByName(String name);
}
