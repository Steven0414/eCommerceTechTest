package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.port.outbound.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Query use case for event sourcing
 * Returns event history for an order
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrderEventsUseCase {
    
    private final EventStore eventStore;
    
    public Flux<DomainEvent> execute(UUID orderId) {
        log.debug("Fetching events for order: {}", orderId);
        return eventStore.getEventsByOrderId(orderId);
    }
}
