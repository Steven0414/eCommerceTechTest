package com.ecommerce.payment.infrastructure.messaging;

import com.ecommerce.payment.application.service.PaymentService;
import com.ecommerce.payment.domain.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for order events - implements Saga pattern
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    
    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DatabaseClient databaseClient;
    
    @Value("${app.kafka.topics.payment-processed}")
    private String paymentProcessedTopic;
    
    @Value("${app.kafka.topics.payment-failed}")
    private String paymentFailedTopic;
    
    @KafkaListener(
        topics = "${app.kafka.topics.order-confirmed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderConfirmed(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventId,
            Acknowledgment acknowledgment
    ) {
        log.info("Received OrderConfirmed event: {}", eventId);
        
        checkIfProcessed(eventId)
            .flatMap(processed -> {
                if (processed) {
                    log.debug("Event {} already processed", eventId);
                    return Mono.empty();
                }
                
                return processOrderConfirmedEvent(message, eventId)
                    .then(markAsProcessed(eventId, "OrderConfirmed"));
            })
            .doOnSuccess(v -> {
                acknowledgment.acknowledge();
                log.info("Order confirmed event processed: {}", eventId);
            })
            .doOnError(error -> log.error("Error processing order confirmed event", error))
            .subscribe();
    }
    
    @SuppressWarnings("unchecked")
    private Mono<Void> processOrderConfirmedEvent(String message, String eventId) {
        return Mono.fromCallable(() -> objectMapper.readValue(message, Map.class))
            .flatMap(eventData -> {
                UUID orderId = UUID.fromString((String) eventData.get("orderId"));
                BigDecimal totalAmount = new BigDecimal(eventData.get("totalAmount").toString());
                
                return paymentService.processPayment(orderId, totalAmount)
                    .flatMap(payment -> publishPaymentResult(payment, eventId));
            });
    }
    
    private Mono<Void> publishPaymentResult(Payment payment, String correlationId) {
        return Mono.fromCallable(() -> {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("orderId", payment.getOrderId().toString());
            event.put("paymentId", payment.getId().toString());
            event.put("amount", payment.getAmount());
            event.put("status", payment.getStatus().name());
            event.put("transactionId", payment.getTransactionId());
            event.put("occurredAt", Instant.now().toString());
            event.put("correlationId", correlationId);
            
String topic = payment.getStatus() == com.ecommerce.payment.domain.model.PaymentStatus.APPROVED 
                ? paymentProcessedTopic 
                : paymentFailedTopic;
            
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.get("eventId").toString(), json);
            
            log.info("Published payment event to topic: {}", topic);
            return null;
        }).then();
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
            .bind("consumerId", "payment-service")
            .then()
            .onErrorResume(error -> {
                log.warn("Event {} might be already processed", eventId);
                return Mono.empty();
            });
    }
}
