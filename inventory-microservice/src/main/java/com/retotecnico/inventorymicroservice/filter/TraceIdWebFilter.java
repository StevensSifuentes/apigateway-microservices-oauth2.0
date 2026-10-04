package com.retotecnico.inventorymicroservice.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString(); // fallback si llega sin trace id (no debería pasar si el Gateway es el único punto público)
        }
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);

        String finalTraceId = traceId;
        return chain.filter(exchange)
                .contextWrite(reactor.util.context.Context.of("X-Trace-Id", finalTraceId));
    }
}