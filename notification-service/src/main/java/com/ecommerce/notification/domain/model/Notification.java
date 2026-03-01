package com.ecommerce.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private UUID orderId;
    private String eventType;
    private String channel;
    private String recipient;
    private String message;
    private Map<String, Object> metadata;
    private NotificationStatus status;
    private Instant sentAt;
    private Instant createdAt;
    
    public static Notification create(UUID orderId, String eventType, String message, Map<String, Object> metadata) {
        return Notification.builder()
            .orderId(orderId)
            .eventType(eventType)
            .channel("EMAIL")
            .recipient("customer@example.com")
            .message(message)
            .metadata(metadata)
            .status(NotificationStatus.PENDING)
            .createdAt(Instant.now())
            .build();
    }
    
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }
}
