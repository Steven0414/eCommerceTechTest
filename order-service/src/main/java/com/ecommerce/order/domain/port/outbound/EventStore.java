package com.ecommerce.order.domain.port.outbound;

import com.ecommerce.order.domain.event.DomainEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Output port for event sourcing store
 */
public interface EventStore {
    Mono<Void> saveEvent(DomainEvent event);
    Flux<DomainEvent> getEventsByOrderId(UUID orderId);
}
