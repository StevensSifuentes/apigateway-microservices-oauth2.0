package com.retotecnico.order_microservice.exception;

import java.util.UUID;

public class OrderCannotBeCancelledException extends RuntimeException {
    public OrderCannotBeCancelledException(UUID id, String motivo) {
        super("No se puede cancelar el pedido " + id + ": " + motivo);
    }
}
