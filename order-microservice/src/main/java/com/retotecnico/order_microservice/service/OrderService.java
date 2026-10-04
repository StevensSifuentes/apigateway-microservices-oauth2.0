package com.retotecnico.order_microservice.service;

import com.retotecnico.order_microservice.domain.Order;
import com.retotecnico.order_microservice.domain.OrderStatusHistory;
import com.retotecnico.order_microservice.dto.CreateExistingOrderRequest;
import com.retotecnico.order_microservice.dto.CreateOrderRequest;
import com.retotecnico.order_microservice.dto.OrderResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrderService {
    Mono<Order> crearPedido(CreateOrderRequest request, UUID userId, String userName);
    Mono<Order> crearConPedidoExistente(CreateExistingOrderRequest request, UUID userId, String userName);
    Mono<Order> findByIdAndUserId(UUID id, UUID userId);
    Flux<OrderResponse> findByUserId(UUID userId);
    Flux<OrderStatusHistory> getHistorial(UUID orderId, UUID userId);
    Mono<Order> cancelar(UUID orderId, UUID userId);
}
