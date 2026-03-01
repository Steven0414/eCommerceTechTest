package com.ecommerce.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Order aggregate - Domain logic
 */
class OrderTest {
    
    @Test
    void shouldCreateOrderWithValidItems() {
        // Given
        UUID customerId = UUID.randomUUID();
        OrderItem item = OrderItem.builder()
            .productId(UUID.randomUUID())
            .productName("Product 1")
            .quantity(2)
            .unitPrice(new BigDecimal("10.00"))
            .build();
        
        // When
        Order order = Order.create(customerId, List.of(item));
        
        // Then
        assertNotNull(order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
        assertEquals(1, order.getDomainEvents().size());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderWithoutItems() {
        // Given
        UUID customerId = UUID.randomUUID();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> Order.create(customerId, List.of()));
    }
    
    @Test
    void shouldTransitionFromPendingToConfirmed() {
        // Given
        Order order = createTestOrder();
        
        // When
        order.changeStatus(OrderStatus.CONFIRMED);
        
        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(2, order.getDomainEvents().size());
    }
    
    @Test
    void shouldNotTransitionFromPaidToConfirmed() {
        // Given
        Order order = createTestOrder();
        order.changeStatus(OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.PAYMENT_PROCESSING);
        order.changeStatus(OrderStatus.PAID);
        
        // When & Then
        assertThrows(IllegalStateException.class, 
            () -> order.changeStatus(OrderStatus.CONFIRMED));
    }
    
    @Test
    void shouldCancelOrderInPendingStatus() {
        // Given
        Order order = createTestOrder();
        
        // When
        order.cancel();
        
        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
    
    @Test
    void shouldNotCancelOrderInPaidStatus() {
        // Given
        Order order = createTestOrder();
        order.changeStatus(OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.PAYMENT_PROCESSING);
        order.changeStatus(OrderStatus.PAID);
        
        // When & Then
        assertThrows(IllegalStateException.class, order::cancel);
    }
    
    @Test
    void shouldCalculateCorrectTotalAmount() {
        // Given
        UUID customerId = UUID.randomUUID();
        OrderItem item1 = createTestItem(new BigDecimal("10.00"), 2);
        OrderItem item2 = createTestItem(new BigDecimal("15.00"), 3);
        
        // When
        Order order = Order.create(customerId, List.of(item1, item2));
        
        // Then
        assertEquals(new BigDecimal("65.00"), order.getTotalAmount());
    }
    
    private Order createTestOrder() {
        UUID customerId = UUID.randomUUID();
        OrderItem item = createTestItem(new BigDecimal("10.00"), 1);
        return Order.create(customerId, List.of(item));
    }
    
    private OrderItem createTestItem(BigDecimal unitPrice, int quantity) {
        return OrderItem.builder()
            .productId(UUID.randomUUID())
            .productName("Test Product")
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
    }
}
