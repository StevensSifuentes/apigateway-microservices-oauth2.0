package com.retotecnico.order_microservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {
    @Id
    private UUID id;
    private UUID userId;
    private String userName;
    private Integer statusId;
    private LocalDateTime orderDate;
    private BigDecimal total;
}
