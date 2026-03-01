package com.ecommerce.order.infrastructure.persistence.r2dbc;

import com.ecommerce.order.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class OrderEntity {
    @Id
    private UUID id;
    private UUID customerId;
    private String status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    
    @Version
    private Long version;
}
