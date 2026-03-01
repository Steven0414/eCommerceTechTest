package com.ecommerce.notification.infrastructure.messaging;

import com.ecommerce.notification.application.service.NotificationService;
import com.ecommerce.notification.domain.model.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = {
            "${app.kafka.topics.order-created}",
            "${app.kafka.topics.order-confirmed}",
            "${app.kafka.topics.order-cancelled}",
            "${app.kafka.topics.payment-processed}",
            "${app.kafka.topics.payment-failed}"
        },
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventId,
            Acknowledgment acknowledgment
    ) {
        log.info("Received event from topic {}: {}", topic, eventId);
        
        processEvent(message, topic)
            .doOnSuccess(notification -> {
                acknowledgment.acknowledge();
                log.info("Notification sent for event: {}", eventId);
            })
            .doOnError(error -> log.error("Error processing event: {}", eventId, error))
            .subscribe();
    }
    
    @SuppressWarnings("unchecked")
    private reactor.core.publisher.Mono<Notification> processEvent(String message, String topic) {
        return reactor.core.publisher.Mono.fromCallable(() -> objectMapper.readValue(message, Map.class))
            .flatMap(eventData -> {
                UUID orderId = UUID.fromString((String) eventData.get("orderId"));
                String eventType = extractEventType(topic);
                String notificationMessage = buildNotificationMessage(eventType, eventData);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("topic", topic);
                metadata.put("eventData", eventData);
                
                Notification notification = Notification.create(orderId, eventType, notificationMessage, metadata);
                return notificationService.sendNotification(notification);
            });
    }
    
    private String extractEventType(String topic) {
        String[] parts = topic.split("\\.");
        return parts[parts.length - 1].toUpperCase();
    }
    
    private String buildNotificationMessage(String eventType, Map<String, Object> eventData) {
        return switch (eventType) {
            case "CREATED" -> "Your order has been created successfully!";
            case "CONFIRMED" -> "Your order has been confirmed and is being processed.";
            case "CANCELLED" -> "Your order has been cancelled.";
            case "PROCESSED" -> "Payment processed successfully for your order.";
            case "FAILED" -> "Payment failed for your order. Please try again.";
            default -> "Order status updated: " + eventType;
        };
    }
}
