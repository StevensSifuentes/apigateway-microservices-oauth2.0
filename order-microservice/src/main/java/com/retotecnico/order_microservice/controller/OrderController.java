package com.retotecnico.order_microservice.controller;

import com.retotecnico.order_microservice.domain.Order;
import com.retotecnico.order_microservice.domain.OrderStatusHistory;
import com.retotecnico.order_microservice.dto.CreateExistingOrderRequest;
import com.retotecnico.order_microservice.dto.CreateOrderRequest;
import com.retotecnico.order_microservice.dto.OrderResponse;
import com.retotecnico.order_microservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    public Mono<ResponseEntity<Order>> create(@RequestBody CreateOrderRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String userName = jwt.getClaimAsString("preferred_username");
        return orderService.crearPedido(request, userId, userName)
                .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Order>> findById(@PathVariable UUID id,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return orderService.findByIdAndUserId(id, userId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}/history")
    public Flux<OrderStatusHistory> history(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return orderService.getHistorial(id, userId);
    }

    @PatchMapping("/{id}/cancel")
    public Mono<ResponseEntity<Order>> cancel(@PathVariable UUID id,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return orderService.cancelar(id, userId)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<OrderResponse> getOrders(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return orderService.findByUserId(userId);
    }

    @PostMapping("/create-existing-order")
    public Mono<ResponseEntity<Order>> createExistingOrder(@RequestBody CreateExistingOrderRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String userName = jwt.getClaimAsString("preferred_username");
        return orderService.crearConPedidoExistente(request, userId, userName)
                .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order));
    }
}
