package com.retotecnico.inventorymicroservice.service;

import com.retotecnico.inventorymicroservice.domain.Product;
import com.retotecnico.inventorymicroservice.exception.InsufficientStockException;
import com.retotecnico.inventorymicroservice.exception.ProductNotFoundException;
import com.retotecnico.inventorymicroservice.repository.ProductRepository;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ProductService implements IProductService
{
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository)
    {
        this.productRepository = productRepository;
    }

    public Mono<Product> save(Product product)
    {
        product.setId(null); // el id es generado por UUID en bd
        return productRepository.save(product);
    }

    public Flux<Product> findAll()
    {
        return productRepository.findAll();
    }

    public Mono<Product> findById(UUID id)
    {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Mono<Product> update(Product product, UUID id)
    {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(p -> {
                    p.setName(product.getName());
                    p.setDescription(product.getDescription());
                    p.setPrice(product.getPrice());
                    p.setStock(product.getStock());
                    p.setStatus(product.getStatus());
                    p.setCategory(product.getCategory());

                    return productRepository.save(p);
                });
    }

    public Mono<Product> delete(UUID id)
    {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(product -> {
                    product.setStatus(false);
                    return productRepository.save(product);
                });
    }

    public Mono<Product> reduceStock(UUID id, Integer cantidad) {
        return productRepository.reduceStock(id, cantidad)
                .flatMap(filasActualizadas -> {
                    if (filasActualizadas == 0) {
                        return Mono.error(new InsufficientStockException(id, cantidad, 0));
                    }
                    return productRepository.findById(id);
                })
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }
}
