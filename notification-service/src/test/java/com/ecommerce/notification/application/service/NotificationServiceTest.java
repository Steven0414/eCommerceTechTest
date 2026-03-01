package com.ecommerce.notification.application.service;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.infrastructure.persistence.NotificationDocument;
import com.ecommerce.notification.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @InjectMocks
    private NotificationService notificationService;
    
    @Test
    void shouldSendNotificationSuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        Notification notification = Notification.create(
            orderId, 
            "OrderCreated", 
            "Order created",
            new HashMap<>()
        );
        
        when(notificationRepository.save(any(NotificationDocument.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        
        // When & Then
        StepVerifier.create(notificationService.sendNotification(notification))
            .expectNextMatches(result -> 
                result.getOrderId().equals(orderId) &&
                result.getStatus() == NotificationStatus.SENT
            )
            .verifyComplete();
    }
    
    @Test
    void shouldGetNotificationsByOrderId() {
        // Given
        UUID orderId = UUID.randomUUID();
        NotificationDocument doc = NotificationDocument.builder()
            .id(UUID.randomUUID().toString())
            .orderId(orderId)
            .eventType("OrderCreated")
            .status("SENT")
            .build();
        
        when(notificationRepository.findByOrderId(orderId))
            .thenReturn(Flux.just(doc));
        
        // When & Then
        StepVerifier.create(notificationService.getNotificationsByOrderId(orderId))
            .expectNextMatches(notification -> 
                notification.getOrderId().equals(orderId)
            )
            .verifyComplete();
    }
}
