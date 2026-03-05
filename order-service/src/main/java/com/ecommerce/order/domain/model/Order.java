package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.event.OrderStatusChangedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private UUID id;
    private UUID customerId;
    private List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    
    @Builder.Default
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    public static Order create(UUID customerId, List<OrderItem> items) {
        // Validate items
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        
        items.forEach(OrderItem::validate);
        items.forEach(OrderItem::calculateSubtotal);
        
        BigDecimal total = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        
        Order order = Order.builder()
            .id(orderId)
            .customerId(customerId)
            .items(items)
            .status(OrderStatus.PENDING)
            .totalAmount(total)
            .createdAt(now)
            .updatedAt(now)
            .version(null)
            .domainEvents(new ArrayList<>())
            .build();
        
        order.addDomainEvent(OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .customerId(customerId)
            .totalAmount(total)
            .status(OrderStatus.PENDING)
            .occurredAt(now)
            .build());
        
        return order;
    }
    
    public void changeStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }
        
        OrderStatus previousStatus = this.status;
        this.status = newStatus;
        this.updatedAt = Instant.now();
        
        addDomainEvent(OrderStatusChangedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(this.id)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .totalAmount(this.totalAmount)
            .occurredAt(this.updatedAt)
            .build());
    }
    
    public void cancel() {
        if (!this.status.isCancellable()) {
            throw new IllegalStateException(
                String.format("Order in status %s cannot be cancelled", this.status)
            );
        }
        changeStatus(OrderStatus.CANCELLED);
    }
    
    public void addDomainEvent(DomainEvent event) {
        if (this.domainEvents == null) {
            this.domainEvents = new ArrayList<>();
        }
        this.domainEvents.add(event);
    }
    
    public void clearDomainEvents() {
        if (this.domainEvents != null) {
            this.domainEvents.clear();
        }
    }
}
