package com.retotecnico.inventorymicroservice.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException
{
    public ProductNotFoundException(UUID id)
    {
        super("Producto no encontrado con id: " + id);
    }
}
