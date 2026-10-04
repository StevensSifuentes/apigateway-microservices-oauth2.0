package com.retotecnico.order_microservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("order_status_history")
public class OrderStatusHistory {
    @Id
    private UUID id;
    private UUID orderId;
    private Integer previusStatusId; // null si es el estado inicial
    private Integer newStatusId;
    private LocalDateTime changedAt;
}
