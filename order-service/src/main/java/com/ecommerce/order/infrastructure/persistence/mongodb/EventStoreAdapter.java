package com.ecommerce.order.infrastructure.persistence.mongodb;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.event.OrderStatusChangedEvent;
import com.ecommerce.order.domain.port.outbound.EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB adapter implementing EventStore port for Event Sourcing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStoreAdapter implements EventStore {
    
    private final MongoEventRepository mongoEventRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> saveEvent(DomainEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            
            UUID aggregateId = extractAggregateId(event);
            
            EventDocument document = EventDocument.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateId(aggregateId)
                .payload(payload)
                .occurredAt(event.getOccurredAt())
                .storedAt(Instant.now())
                .build();
            
            return mongoEventRepository.save(document)
                .doOnSuccess(saved -> log.debug("Event stored: {}", event.getEventType()))
                .then();
        } catch (Exception e) {
            log.error("Error saving event to store", e);
            return Mono.error(e);
        }
    }
    
    @Override
    public Flux<DomainEvent> getEventsByOrderId(UUID orderId) {
        return mongoEventRepository.findByAggregateIdOrderByOccurredAt(orderId)
            .map(this::toDomainEvent)
            .doOnComplete(() -> log.debug("Retrieved events for order: {}", orderId));
    }
    
    private UUID extractAggregateId(DomainEvent event) {
        if (event instanceof OrderCreatedEvent) {
            return ((OrderCreatedEvent) event).getOrderId();
        } else if (event instanceof OrderStatusChangedEvent) {
            return ((OrderStatusChangedEvent) event).getOrderId();
        }
        throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
    }
    
    private DomainEvent toDomainEvent(EventDocument document) {
        try {
            Class<?> eventClass = switch (document.getEventType()) {
                case "OrderCreated" -> OrderCreatedEvent.class;
                case "OrderStatusChanged" -> OrderStatusChangedEvent.class;
                default -> throw new IllegalArgumentException("Unknown event type: " + document.getEventType());
            };
            
            return (DomainEvent) objectMapper.convertValue(document.getPayload(), eventClass);
        } catch (Exception e) {
            log.error("Error converting event document", e);
            throw new RuntimeException(e);
        }
    }
}
