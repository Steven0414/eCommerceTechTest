package com.ecommerce.order.infrastructure.messaging.kafka;

import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for payment events with idempotency
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${app.kafka.topics.payment-processed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentProcessed(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventId,
            Acknowledgment acknowledgment
    ) {
        log.info("Received PaymentProcessed event: {}", eventId);
        
        idempotencyService.processEvent(eventId, "PaymentProcessed", () -> 
            processPaymentEvent(message, true)
        )
        .doOnSuccess(v -> {
            acknowledgment.acknowledge();
            log.info("Payment processed event handled: {}", eventId);
        })
        .doOnError(error -> log.error("Error processing payment event: {}", eventId, error))
        .subscribe();
    }
    
    @KafkaListener(
        topics = "${app.kafka.topics.payment-failed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventId,
            Acknowledgment acknowledgment
    ) {
        log.info("Received PaymentFailed event: {}", eventId);
        
        idempotencyService.processEvent(eventId, "PaymentFailed", () -> 
            processPaymentEvent(message, false)
        )
        .doOnSuccess(v -> {
            acknowledgment.acknowledge();
            log.info("Payment failed event handled: {}", eventId);
        })
        .doOnError(error -> log.error("Error processing payment failed event: {}", eventId, error))
        .subscribe();
    }
    
    @SuppressWarnings("unchecked")
    private Mono<Void> processPaymentEvent(String message, boolean success) {
        return Mono.fromCallable(() -> objectMapper.readValue(message, Map.class))
            .flatMap(eventData -> {
                UUID orderId = UUID.fromString((String) eventData.get("orderId"));
                return orderRepository.findById(orderId)
                    .flatMap(order -> {
                        OrderStatus newStatus = success ? OrderStatus.PAID : OrderStatus.FAILED;
                        order.changeStatus(newStatus);
                        return orderRepository.save(order);
                    });
            })
            .then()
            .onErrorResume(error -> {
                log.error("Error updating order status", error);
                return Mono.empty();
            });
    }
}
