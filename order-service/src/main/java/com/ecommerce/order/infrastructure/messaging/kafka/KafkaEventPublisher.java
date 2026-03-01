package com.ecommerce.order.infrastructure.messaging.kafka;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.port.outbound.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Kafka adapter implementing EventPublisher port
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;
    
    @Value("${app.kafka.topics.order-confirmed}")
    private String orderConfirmedTopic;
    
    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;
    
    @Override
    public Mono<Void> publish(DomainEvent event) {
        return Mono.fromCallable(() -> {
            String topic = getTopicForEvent(event.getEventType());
            String json = objectMapper.writeValueAsString(event);
            
            log.info("Publishing event {} to topic {}", event.getEventType(), topic);
            
            kafkaTemplate.send(topic, event.getEventId(), json);
            return null;
        })
        .then()
        .doOnError(error -> log.error("Error publishing event: {}", event.getEventType(), error));
    }
    
    private String getTopicForEvent(String eventType) {
        return switch (eventType) {
            case "OrderCreated" -> orderCreatedTopic;
            case "OrderStatusChanged" -> orderConfirmedTopic;
            default -> orderCreatedTopic;
        };
    }
}
