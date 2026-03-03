package com.ecommerce.notification.infrastructure.persistence;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.port.outbound.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter for notification repository - implements domain port
 */
@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {
    
    private final NotificationMongoRepository mongoRepository;
    
    @Override
    public Mono<Notification> save(Notification notification) {
        NotificationDocument document = toDocument(notification);
        return mongoRepository.save(document)
            .map(this::toDomain);
    }
    
    @Override
    public Flux<Notification> findByOrderId(UUID orderId) {
        return mongoRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }
    
    private NotificationDocument toDocument(Notification notification) {
        return NotificationDocument.builder()
            .id(notification.getId())
            .orderId(notification.getOrderId())
            .eventType(notification.getEventType())
            .channel(notification.getChannel())
            .recipient(notification.getRecipient())
            .message(notification.getMessage())
            .metadata(notification.getMetadata())
            .status(notification.getStatus().name())
            .sentAt(notification.getSentAt())
            .createdAt(notification.getCreatedAt())
            .build();
    }
    
    private Notification toDomain(NotificationDocument document) {
        return Notification.builder()
            .id(document.getId())
            .orderId(document.getOrderId())
            .eventType(document.getEventType())
            .channel(document.getChannel())
            .recipient(document.getRecipient())
            .message(document.getMessage())
            .metadata(document.getMetadata())
            .status(NotificationStatus.valueOf(document.getStatus()))
            .sentAt(document.getSentAt())
            .createdAt(document.getCreatedAt())
            .build();
    }
}
