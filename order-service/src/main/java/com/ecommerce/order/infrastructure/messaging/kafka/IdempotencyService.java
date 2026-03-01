package com.ecommerce.order.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Service to ensure idempotent event processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final DatabaseClient databaseClient;
    
    public Mono<Void> processEvent(String eventId, String eventType, Supplier<Mono<Void>> processor) {
        return checkIfProcessed(eventId)
            .flatMap(processed -> {
                if (processed) {
                    log.debug("Event {} already processed, skipping", eventId);
                    return Mono.empty();
                }
                
                return processor.get()
                    .then(markAsProcessed(eventId, eventType));
            });
    }
    
    private Mono<Boolean> checkIfProcessed(String eventId) {
        return databaseClient.sql(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = :eventId"
            )
            .bind("eventId", UUID.fromString(eventId))
            .map(row -> row.get(0, Long.class))
            .one()
            .map(count -> count > 0)
            .onErrorReturn(false);
    }
    
    private Mono<Void> markAsProcessed(String eventId, String eventType) {
        return databaseClient.sql(
                "INSERT INTO processed_events (event_id, event_type, processed_at, consumer_id) " +
                "VALUES (:eventId, :eventType, :processedAt, :consumerId)"
            )
            .bind("eventId", UUID.fromString(eventId))
            .bind("eventType", eventType)
            .bind("processedAt", Instant.now())
            .bind("consumerId", "order-service")
            .then()
            .onErrorResume(error -> {
                log.warn("Event {} might be already processed", eventId);
                return Mono.empty();
            });
    }
}
