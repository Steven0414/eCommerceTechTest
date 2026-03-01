package com.ecommerce.order.domain.port.outbound;

import com.ecommerce.order.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Output port for order persistence following Hexagonal Architecture
 */
public interface OrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(UUID orderId);
    Flux<Order> findByCustomerId(UUID customerId);
    Mono<Boolean> existsById(UUID orderId);
}
