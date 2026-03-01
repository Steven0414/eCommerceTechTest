package com.ecommerce.order.infrastructure.persistence.r2dbc;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * R2DBC adapter implementing OrderRepository port
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    
    private final R2dbcOrderRepository orderRepository;
    private final R2dbcOrderItemRepository orderItemRepository;
    
    @Override
    public Mono<Order> save(Order order) {
        OrderEntity entity = toEntity(order);
        
        return orderRepository.save(entity)
            .flatMap(savedEntity -> {
                List<OrderItemEntity> itemEntities = order.getItems().stream()
                    .map(item -> toItemEntity(item, savedEntity.getId()))
                    .collect(Collectors.toList());
                
                return orderItemRepository.saveAll(itemEntities)
                    .collectList()
                    .map(savedItems -> toDomain(savedEntity, savedItems, order.getDomainEvents()));
            });
    }
    
    @Override
    public Mono<Order> findById(UUID orderId) {
        return orderRepository.findById(orderId)
            .flatMap(entity -> 
                orderItemRepository.findByOrderId(orderId)
                    .collectList()
                    .map(items -> toDomain(entity, items, List.of()))
            );
    }
    
    @Override
    public Flux<Order> findByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
            .flatMap(entity ->
                orderItemRepository.findByOrderId(entity.getId())
                    .collectList()
                    .map(items -> toDomain(entity, items, List.of()))
            );
    }
    
    @Override
    public Mono<Boolean> existsById(UUID orderId) {
        return orderRepository.existsById(orderId);
    }
    
    private OrderEntity toEntity(Order order) {
        return OrderEntity.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .status(order.getStatus().name())
            .totalAmount(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .version(order.getVersion())
            .build();
    }
    
    private OrderItemEntity toItemEntity(OrderItem item, UUID orderId) {
        return OrderItemEntity.builder()
            .id(item.getId())
            .orderId(orderId)
            .productId(item.getProductId())
            .productName(item.getProductName())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .subtotal(item.getSubtotal())
            .build();
    }
    
    private Order toDomain(OrderEntity entity, List<OrderItemEntity> itemEntities, 
                          List<com.ecommerce.order.domain.event.DomainEvent> events) {
        List<OrderItem> items = itemEntities.stream()
            .map(this::toItemDomain)
            .collect(Collectors.toList());
        
        return Order.builder()
            .id(entity.getId())
            .customerId(entity.getCustomerId())
            .items(items)
            .status(OrderStatus.valueOf(entity.getStatus()))
            .totalAmount(entity.getTotalAmount())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .version(entity.getVersion())
            .domainEvents(events)
            .build();
    }
    
    private OrderItem toItemDomain(OrderItemEntity entity) {
        return OrderItem.builder()
            .id(entity.getId())
            .productId(entity.getProductId())
            .productName(entity.getProductName())
            .quantity(entity.getQuantity())
            .unitPrice(entity.getUnitPrice())
            .subtotal(entity.getSubtotal())
            .build();
    }
}
