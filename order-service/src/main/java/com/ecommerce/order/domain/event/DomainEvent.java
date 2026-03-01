package com.ecommerce.order.domain.event;

import java.time.Instant;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    Instant getOccurredAt();
}
