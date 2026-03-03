package com.ecommerce.notification.application.service;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.port.outbound.NotificationRepository;
import com.ecommerce.notification.domain.port.outbound.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    
    public Mono<Notification> sendNotification(Notification notification) {
        log.info("Sending notification for order {} - event {}", 
            notification.getOrderId(), notification.getEventType());
        
        return notificationSender.send(notification)
            .flatMap(success -> {
                if (success) {
                    notification.markAsSent();
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                }
                return notificationRepository.save(notification);
            });
    }
    
    public Flux<Notification> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
}
