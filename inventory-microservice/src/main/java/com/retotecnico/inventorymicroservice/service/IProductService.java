package com.retotecnico.inventorymicroservice.service;

import com.retotecnico.inventorymicroservice.domain.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IProductService
{
    Mono<Product> save(Product product);
    Flux<Product> findAll();
    Mono<Product> findById(UUID id);
    Mono<Product> update(Product product, UUID id);
    Mono<Product> delete(UUID id);
    Mono<Product> reduceStock(UUID id, Integer cantidad);
}
