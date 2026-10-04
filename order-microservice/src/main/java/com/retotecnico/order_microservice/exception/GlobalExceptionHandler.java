package com.retotecnico.order_microservice.exception;

import com.retotecnico.order_microservice.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericError(Exception ex, ServerWebExchange exchange) {
        String traceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), 500, ex.getMessage(), traceId);
        return Mono.just(ResponseEntity.status(500).body(error));
    }

}
