package com.retotecnico.order_microservice.service;

import com.retotecnico.order_microservice.domain.Order;
import com.retotecnico.order_microservice.domain.OrderItem;
import com.retotecnico.order_microservice.domain.OrderStatusHistory;
import com.retotecnico.order_microservice.dto.*;
import com.retotecnico.order_microservice.exception.InsufficientStockException;
import com.retotecnico.order_microservice.exception.OrderCannotBeCancelledException;
import com.retotecnico.order_microservice.exception.OrderNotFoundException;
import com.retotecnico.order_microservice.exception.ProductNotAvailableException;
import com.retotecnico.order_microservice.repository.OrderItemRepository;
import com.retotecnico.order_microservice.repository.OrderRepository;
import com.retotecnico.order_microservice.repository.OrderStatusHistoryRepository;
import com.retotecnico.order_microservice.repository.OrderStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final WebClient inventarioClient;


    public OrderServiceImpl(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusRepository orderStatusRepository,
                        OrderStatusHistoryRepository historyRepository,
                        WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.historyRepository = historyRepository;
        this.inventarioClient = webClientBuilder.baseUrl("http://inventario-service:8080").build();
    }

    public Mono<Order> crearPedido(CreateOrderRequest request, UUID userId, String userName) {
        return Flux.fromIterable(request.items())
                .flatMap(itemReq -> verificarProducto(itemReq.productId(), itemReq.quantity())
                        .flatMap(product -> reducirStockEnInventario(itemReq.productId(), itemReq.quantity())
                                .thenReturn(construirOrderItem(product, itemReq.quantity()))))
                .collectList()
                .flatMap(items -> guardarPedidoConItems(items, userId, userName));
    }

    private Mono<ProductDTO> verificarProducto(UUID productId, Integer cantidadSolicitada) {
        return getCurrentToken()
                .flatMap(token -> inventarioClient.get()
                        .uri("/api/v1/products/{id}", productId)
                        .headers(headers -> {
                            headers.setBearerAuth(token);
                        })
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError,
                                response -> Mono.error(new ProductNotAvailableException(productId, "Producto no encontrado.")))
                        .bodyToMono(ProductDTO.class)
                        .timeout(Duration.ofSeconds(3))
                        .flatMap(product -> {
                            if (!product.status()) {
                                return Mono.error(new ProductNotAvailableException(productId, "Producto inactivo."));
                            }
                            if (product.stock() < cantidadSolicitada) {
                                return Mono.error(new InsufficientStockException(productId, cantidadSolicitada, product.stock()));
                            }
                            return Mono.just(product);
                        })
                );
    }

    private OrderItem construirOrderItem(ProductDTO product, Integer cantidad) {
        OrderItem item = new OrderItem();
        item.setProductId(product.id());
        item.setProductName(product.name());
        item.setUnitPrice(product.price());
        item.setQuantity(cantidad);
        item.setSubtotal(product.price().multiply(BigDecimal.valueOf(cantidad)));
        return item;
    }

    private Mono<Order> guardarPedidoConItems(List<OrderItem> items, UUID userId, String userName) {
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return orderStatusRepository.findByName("CONFIRMED")
                .switchIfEmpty(Mono.error(new IllegalStateException("Estado CONFIRMED no configurado.")))
                .flatMap(estadoInicial -> {
                    Order order = new Order();
                    order.setUserId(userId);
                    order.setUserName(userName);
                    order.setOrderDate(LocalDateTime.now());
                    order.setStatusId(estadoInicial.getId());
                    order.setTotal(total);

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> guardarItems(savedOrder.getId(), items)
                                    .then(registrarHistorial(savedOrder.getId(), null, estadoInicial.getId()))
                                    .thenReturn(savedOrder));
                });
    }

    private Mono<Void> guardarItems(UUID orderId, List<OrderItem> items) {
        items.forEach(item -> item.setOrderId(orderId));
        return orderItemRepository.saveAll(items).then();
    }

    private Mono<Void> reducirStockEnInventario(UUID productId, Integer cantidad){
        return getCurrentToken()
                .flatMap(token -> inventarioClient.patch()
                        .uri("/api/v1/products/{id}/reduce-stock", productId)
                        .headers(headers -> headers.setBearerAuth(token))
                        .bodyValue(new ReduceStockRequest(cantidad))
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError,
                                response -> Mono.error(new InsufficientStockException(productId, cantidad, 0)))
                        .bodyToMono(Void.class)
                        .timeout(Duration.ofSeconds(3)));
    }

    private Mono<String> getCurrentToken(){
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> auth.getToken().getTokenValue());
    }

    public Mono<Order> findByIdAndUserId(UUID id, UUID userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(id)));
    }

    public Flux<OrderResponse> findByUserId(UUID userId) {
        return orderRepository.findByUserId(userId)
                .map(order -> new OrderResponse(
                        order.getId(),
                        order.getUserId(),
                        order.getUserName(),
                        order.getStatusId(),
                        order.getOrderDate(),
                        order.getTotal(),
                        ""
                ));
    }

    public Flux<OrderStatusHistory> getHistorial(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMapMany(order -> historyRepository.findByOrderIdOrderByChangedAtAsc(orderId));
    }

    public Mono<Order> cancelar(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order ->
                        orderStatusRepository.findById(order.getStatusId())
                                .flatMap(estadoActual -> {
                                    if (estadoActual.getName().equals("DELIVERED")) {
                                        return Mono.error(new OrderCannotBeCancelledException(orderId, "El pedido ya fue entregado."));
                                    }
                                    if (estadoActual.getName().equals("CANCELLED")) {
                                        return Mono.error(new OrderCannotBeCancelledException(orderId, "El pedido ya está cancelado."));
                                    }
                                    return cambiarEstado(order, "CANCELLED");
                                })
                );
    }

    private Mono<Order> cambiarEstado(Order order, String nuevoEstadoNombre) {
        return orderStatusRepository.findByName(nuevoEstadoNombre)
                .switchIfEmpty(Mono.error(new IllegalStateException("Estado no válido: " + nuevoEstadoNombre)))
                .flatMap(nuevoEstado -> {
                    Integer estadoAnteriorId = order.getStatusId();
                    order.setStatusId(nuevoEstado.getId());

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> registrarHistorial(savedOrder.getId(), estadoAnteriorId, nuevoEstado.getId())
                                    .thenReturn(savedOrder));
                });
    }

    private Mono<OrderStatusHistory> registrarHistorial(UUID orderId, Integer statusAnteriorId, Integer statusNuevoId) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setPreviusStatusId(statusAnteriorId);
        history.setNewStatusId(statusNuevoId);
        history.setChangedAt(LocalDateTime.now());
        return historyRepository.save(history);
    }

    public Mono<Order> crearConPedidoExistente(CreateExistingOrderRequest request, UUID userId, String userName) {
        return orderRepository.findByIdAndUserId(request.orderId(), userId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(userId)))
                .flatMapMany(order -> orderItemRepository.findByOrderId(order.getId()))
                .concatMap(existingItem -> verificarProducto(existingItem.getProductId(), existingItem.getQuantity())
                        .flatMap(product -> reducirStockEnInventario(existingItem.getProductId(), existingItem.getQuantity())
                                .thenReturn(construirOrderItem(product, existingItem.getQuantity()))))
                .collectList()
                .flatMap(items -> guardarPedidoConItems(items, userId, userName));
    }
}
