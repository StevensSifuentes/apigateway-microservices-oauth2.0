package com.retotecnico.inventorymicroservice.controller;

import com.retotecnico.inventorymicroservice.domain.Product;
import com.retotecnico.inventorymicroservice.dto.ReduceStockRequest;
import com.retotecnico.inventorymicroservice.service.IProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController
{
    private final IProductService productService;

    public ProductController(IProductService productService)
    {
        this.productService = productService;
    }

    @PostMapping
    public Mono<ResponseEntity<Product>> create(@RequestBody Product product)
    {
        return productService.save(product)
                .map(saved -> new ResponseEntity<>(saved, HttpStatus.CREATED));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Product> findAll()
    {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Product> findById(@PathVariable UUID id)
    {
        return productService.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> update(@RequestBody Product product, @PathVariable UUID id)
    {
        return productService.update(product, id)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Product>> delete(@PathVariable UUID id)
    {
        return productService.delete(id)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}/reduce-stock")
    public Mono<ResponseEntity<Product>> reduceStock(@PathVariable UUID id,
                                                      @RequestBody ReduceStockRequest request) {
        return productService.reduceStock(id, request.quantity())
                .map(ResponseEntity::ok);
    }
}
