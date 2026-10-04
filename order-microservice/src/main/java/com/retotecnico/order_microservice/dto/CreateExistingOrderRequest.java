package com.retotecnico.order_microservice.dto;

import java.util.UUID;

public record CreateExistingOrderRequest(UUID orderId) { }
