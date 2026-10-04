package com.retotecnico.inventorymicroservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@Table(name = "products")
public class Product
{
    @Id
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Boolean status;
    private Category category;

    @Version
    private Long version;
}
