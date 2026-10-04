package com.retotecnico.inventorymicroservice.repository;

import com.retotecnico.inventorymicroservice.domain.Product;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductRepository extends R2dbcRepository<Product, UUID> {
    @Modifying
    @Query("""
            UPDATE products
            SET stock = stock - :cantidad 
            WHERE id = :id AND stock >= :cantidad
            """)
    Mono<Integer> reduceStock(UUID id, Integer cantidad);
}
