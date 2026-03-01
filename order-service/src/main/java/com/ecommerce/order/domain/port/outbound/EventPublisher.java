package com.ecommerce.order.domain.port.outbound;

import com.ecommerce.order.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

/**
 * Output port for publishing domain events
 */
public interface EventPublisher {
    Mono<Void> publish(DomainEvent event);
}
