package com.ecommerce.notification.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class NotificationDocument {
    @Id
    private String id;
    private UUID orderId;
    private String eventType;
    private String channel;
    private String recipient;
    private String message;
    private Map<String, Object> metadata;
    private String status;
    private Instant sentAt;
    private Instant createdAt;
}
