package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements DomainEvent {
    private String eventId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant occurredAt;
    
    @Override
    public String getEventType() {
        return "OrderCreated";
    }
}
