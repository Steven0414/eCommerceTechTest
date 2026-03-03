package com.ecommerce.notification.infrastructure.messaging;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.port.outbound.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Adapter for sending notifications - implements domain port
 * In a real scenario, this would integrate with email/SMS services
 */
@Slf4j
@Component
public class NotificationSenderAdapter implements NotificationSender {
    
    @Override
    public Mono<Boolean> send(Notification notification) {
        log.info("Simulating notification send for order {} - channel: {}", 
            notification.getOrderId(), notification.getChannel());
        
        // Simulates 500ms delivery time
        return Mono.delay(Duration.ofMillis(500))
            .then(Mono.fromCallable(() -> {
                log.info("Notification sent successfully to {}", notification.getRecipient());
                return true;
            }))
            .onErrorReturn(false);
    }
}
