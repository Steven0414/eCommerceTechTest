package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query use case - CQRS pattern
 * Handles order queries without side effects
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrderUseCase {
    
    private final OrderRepository orderRepository;
    
    public Mono<OrderResponse> execute(UUID orderId) {
        log.debug("Fetching order: {}", orderId);
        
        return orderRepository.findById(orderId)
            .map(this::mapToResponse)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found: " + orderId)));
    }
    
    public Flux<OrderResponse> executeByCustomer(UUID customerId) {
        log.debug("Fetching orders for customer: {}", customerId);
        
        return orderRepository.findByCustomerId(customerId)
            .map(this::mapToResponse);
    }
    
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .items(order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
    
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
}
