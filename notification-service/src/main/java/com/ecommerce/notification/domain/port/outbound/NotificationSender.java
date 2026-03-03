package com.ecommerce.notification.domain.port.outbound;

import com.ecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Mono;

/**
 * Output port for sending notifications
 */
public interface NotificationSender {
    Mono<Boolean> send(Notification notification);
}
