package com.retotecnico.order_microservice.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("order_status")
public class OrderStatus {
    @Id
    private Integer id;
    private String name;
}