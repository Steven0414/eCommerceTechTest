package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.port.outbound.EventPublisher;
import com.ecommerce.order.domain.port.outbound.EventStore;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command use case - CQRS pattern
 * Handles order creation with event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final EventStore eventStore;
    
    public Mono<OrderResponse> execute(CreateOrderCommand command) {
        log.info("Creating order for customer: {}", command.getCustomerId());
        
        return Mono.fromCallable(() -> mapToOrderItems(command.getItems()))
            .flatMap(items -> Mono.fromCallable(() -> Order.create(command.getCustomerId(), items)))
            .flatMap(orderRepository::save)
            .flatMap(this::publishDomainEvents)
            .map(this::mapToResponse)
            .doOnSuccess(response -> log.info("Order created successfully: {}", response.getId()))
            .doOnError(error -> log.error("Error creating order", error));
    }
    
    private List<OrderItem> mapToOrderItems(List<CreateOrderCommand.OrderItemDto> items) {
        return items.stream()
            .map(item -> OrderItem.builder()
                .id(UUID.randomUUID())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build())
            .collect(Collectors.toList());
    }
    
    private Mono<Order> publishDomainEvents(Order order) {
        List<DomainEvent> events = order.getDomainEvents();
        
        return Flux.fromIterable(events)
            .flatMap(event -> 
                eventStore.saveEvent(event)
                    .then(eventPublisher.publish(event))
                    .doOnSuccess(v -> log.debug("Published event: {}", event.getEventType()))
            )
            .then(Mono.defer(() -> {
                order.clearDomainEvents();
                return Mono.just(order);
            }));
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
}
