package com.ecommerce.order.infrastructure.persistence.mongodb;

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
@Document(collection = "domain_events")
public class EventDocument {
    @Id
    private String id;
    private String eventId;
    private String eventType;
    private UUID aggregateId;
    private Map<String, Object> payload;
    private Instant occurredAt;
    private Instant storedAt;
}
