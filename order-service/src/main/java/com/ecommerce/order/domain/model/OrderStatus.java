package com.ecommerce.order.domain.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_PROCESSING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED || newStatus == FAILED;
            case CONFIRMED -> newStatus == PAYMENT_PROCESSING || newStatus == CANCELLED || newStatus == FAILED;
            case PAYMENT_PROCESSING -> newStatus == PAID || newStatus == FAILED;
            case PAID -> newStatus == SHIPPED || newStatus == FAILED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED, CANCELLED, FAILED -> false;
        };
    }
    
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }
}
