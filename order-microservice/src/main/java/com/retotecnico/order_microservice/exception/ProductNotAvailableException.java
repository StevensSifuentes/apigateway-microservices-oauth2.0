package com.retotecnico.order_microservice.exception;

import java.util.UUID;

public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException(UUID productId, String motivo) {
        super("Producto " + productId + " no disponible: " + motivo);
    }
}
