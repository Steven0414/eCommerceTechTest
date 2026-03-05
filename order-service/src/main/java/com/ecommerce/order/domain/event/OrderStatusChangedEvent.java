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
public class OrderStatusChangedEvent implements DomainEvent {
    private String eventId;
    private UUID orderId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private BigDecimal totalAmount;
    private Instant occurredAt;
    
    @Override
    public String getEventType() {
        return "OrderStatusChanged";
    }
}
