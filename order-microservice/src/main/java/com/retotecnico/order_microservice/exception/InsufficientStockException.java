package com.retotecnico.order_microservice.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID productId, int solicitado, int disponible) {
        super("Stock insuficiente para producto " + productId +
                ": solicitado " + solicitado + ", disponible " + disponible);
    }
}
