package com.retotecnico.order_microservice.service;

import com.retotecnico.order_microservice.domain.Order;
import com.retotecnico.order_microservice.domain.OrderItem;
import com.retotecnico.order_microservice.domain.OrderStatus;
import com.retotecnico.order_microservice.domain.OrderStatusHistory;
import com.retotecnico.order_microservice.dto.CreateExistingOrderRequest;
import com.retotecnico.order_microservice.dto.ProductDTO;
import com.retotecnico.order_microservice.exception.InsufficientStockException;
import com.retotecnico.order_microservice.exception.OrderNotFoundException;
import com.retotecnico.order_microservice.exception.ProductNotAvailableException;
import com.retotecnico.order_microservice.repository.OrderItemRepository;
import com.retotecnico.order_microservice.repository.OrderRepository;
import com.retotecnico.order_microservice.repository.OrderStatusHistoryRepository;
import com.retotecnico.order_microservice.repository.OrderStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del flujo de "reorder": crearConPedidoExistente.
 * <p>
 * Nota: OrderServiceImpl construye su propio WebClient internamente a partir
 * del WebClient.Builder recibido, por lo que aquí se simula toda la cadena
 * fluida (.get()/.patch().uri().headers()...) en vez de mockear un WebClient
 * ya construido.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplReorderTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusRepository orderStatusRepository;
    @Mock
    private OrderStatusHistoryRepository historyRepository;

    private WebClient.RequestHeadersUriSpec<?> getUriSpec;
    private WebClient.RequestBodyUriSpec patchUriSpec;

    private OrderServiceImpl orderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        lenient().doReturn(getUriSpec).when(webClient).get();
        lenient().when(webClient.patch()).thenReturn(patchUriSpec);

        orderService = new OrderServiceImpl(
                orderRepository, orderItemRepository, orderStatusRepository, historyRepository, webClientBuilder
        );
    }

    // ==========================================================
    // Helpers de datos y de simulación de la llamada a Inventario
    // ==========================================================

    private JwtAuthenticationToken jwtAuth() {
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    private OrderItem existingItem(UUID productId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    /** Simula GET /api/v1/products/{id} devolviendo el producto dado. */
    private void mockVerificarProducto(ProductDTO product) {
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(headersSpec).when(getUriSpec).uri(eq("/api/v1/products/{id}"), eq(product.id()));
        doReturn(headersSpec).when(headersSpec).headers(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ProductDTO.class)).thenReturn(Mono.just(product));
    }

    /** Simula PATCH /api/v1/products/{id}/reduce-stock respondiendo OK (sin cuerpo). */
    private void mockReducirStockOk(UUID productId) {
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(patchUriSpec.uri(eq("/api/v1/products/{id}/reduce-stock"), eq(productId))).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    // ==========================================================
    // Casos de prueba
    // ==========================================================

    @Test
    @DisplayName("reorder exitoso: recrea el pedido con los items del original, verificando y descontando stock")
    void crearConPedidoExistente_exitoso() {
        Order pedidoOriginal = new Order();
        pedidoOriginal.setId(orderId);
        pedidoOriginal.setUserId(userId);

        OrderItem item = existingItem(productId, 2);
        ProductDTO product = new ProductDTO(productId, "Mouse", BigDecimal.valueOf(50), 10, true);

        OrderStatus estadoConfirmed = new OrderStatus();
        estadoConfirmed.setId(1);
        estadoConfirmed.setName("CONFIRMED");

        Order nuevoPedidoGuardado = new Order();
        nuevoPedidoGuardado.setId(UUID.randomUUID());

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.just(pedidoOriginal));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(item));

        mockVerificarProducto(product);
        mockReducirStockOk(productId);

        when(orderStatusRepository.findByName("CONFIRMED")).thenReturn(Mono.just(estadoConfirmed));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(nuevoPedidoGuardado));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.empty());
        when(historyRepository.save(any(OrderStatusHistory.class))).thenReturn(Mono.just(new OrderStatusHistory()));

        CreateExistingOrderRequest request = new CreateExistingOrderRequest(orderId);

        StepVerifier.create(
                        orderService.crearConPedidoExistente(request, userId, "Usuario Test")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuth()))
                )
                .expectNextMatches(order -> order.getId().equals(nuevoPedidoGuardado.getId()))
                .verifyComplete();

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(historyRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("reorder con múltiples items: verifica y descuenta stock de todos, y calcula el total correcto")
    void crearConPedidoExistente_multiplesItems() {
        UUID productId2 = UUID.randomUUID();

        Order pedidoOriginal = new Order();
        pedidoOriginal.setId(orderId);
        pedidoOriginal.setUserId(userId);

        OrderItem item1 = existingItem(productId, 2);
        OrderItem item2 = existingItem(productId2, 1);

        ProductDTO product1 = new ProductDTO(productId, "Mouse", BigDecimal.valueOf(50), 10, true);
        ProductDTO product2 = new ProductDTO(productId2, "Teclado", BigDecimal.valueOf(100), 5, true);

        OrderStatus estadoConfirmed = new OrderStatus();
        estadoConfirmed.setId(1);
        estadoConfirmed.setName("CONFIRMED");

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.just(pedidoOriginal));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(item1, item2));

        mockVerificarProducto(product1);
        mockVerificarProducto(product2);
        mockReducirStockOk(productId);
        mockReducirStockOk(productId2);

        when(orderStatusRepository.findByName("CONFIRMED")).thenReturn(Mono.just(estadoConfirmed));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return Mono.just(o);
        });
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.empty());
        when(historyRepository.save(any(OrderStatusHistory.class))).thenReturn(Mono.just(new OrderStatusHistory()));

        CreateExistingOrderRequest request = new CreateExistingOrderRequest(orderId);

        // Total esperado: (2 * 50) + (1 * 100) = 200
        StepVerifier.create(
                        orderService.crearConPedidoExistente(request, userId, "Usuario Test")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuth()))
                )
                .expectNextMatches(order -> order.getTotal().compareTo(BigDecimal.valueOf(200)) == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("reorder falla si el pedido original no existe (o no pertenece al usuario)")
    void crearConPedidoExistente_pedidoOriginalNoExiste() {
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.empty());

        CreateExistingOrderRequest request = new CreateExistingOrderRequest(orderId);

        StepVerifier.create(
                        orderService.crearConPedidoExistente(request, userId, "Usuario Test")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuth()))
                )
                .expectError(OrderNotFoundException.class)
                .verify();

        verify(orderItemRepository, never()).findByOrderId(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("reorder falla si un producto ya no tiene stock suficiente")
    void crearConPedidoExistente_stockInsuficiente() {
        Order pedidoOriginal = new Order();
        pedidoOriginal.setId(orderId);
        pedidoOriginal.setUserId(userId);

        OrderItem item = existingItem(productId, 5);
        ProductDTO product = new ProductDTO(productId, "Mouse", BigDecimal.valueOf(50), 2, true); // stock insuficiente

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.just(pedidoOriginal));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(item));

        mockVerificarProducto(product);

        CreateExistingOrderRequest request = new CreateExistingOrderRequest(orderId);

        StepVerifier.create(
                        orderService.crearConPedidoExistente(request, userId, "Usuario Test")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuth()))
                )
                .expectError(InsufficientStockException.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("reorder falla si un producto ya no está activo/disponible")
    void crearConPedidoExistente_productoInactivo() {
        Order pedidoOriginal = new Order();
        pedidoOriginal.setId(orderId);
        pedidoOriginal.setUserId(userId);

        OrderItem item = existingItem(productId, 1);
        ProductDTO product = new ProductDTO(productId, "Mouse", BigDecimal.valueOf(50), 10, false); // inactivo

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.just(pedidoOriginal));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(item));

        mockVerificarProducto(product);

        CreateExistingOrderRequest request = new CreateExistingOrderRequest(orderId);

        StepVerifier.create(
                        orderService.crearConPedidoExistente(request, userId, "Usuario Test")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuth()))
                )
                .expectError(ProductNotAvailableException.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }
}