package com.ecommerce.notification.application.service;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.infrastructure.persistence.NotificationDocument;
import com.ecommerce.notification.infrastructure.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public Mono<Notification> sendNotification(Notification notification) {
        log.info("Sending notification for order {} - event {}", 
            notification.getOrderId(), notification.getEventType());
        
        return simulateNotificationSending()
            .flatMap(success -> {
                if (success) {
                    notification.markAsSent();
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                }
                return saveNotification(notification);
            });
    }
    
    public Flux<Notification> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }
    
    private Mono<Notification> saveNotification(Notification notification) {
        NotificationDocument document = toDocument(notification);
        return notificationRepository.save(document)
            .map(this::toDomain);
    }
    
    private Mono<Boolean> simulateNotificationSending() {
        return Mono.delay(Duration.ofMillis(500))
            .map(tick -> true);
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
