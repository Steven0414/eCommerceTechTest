package com.ecommerce.notification.domain.port.outbound;

import com.ecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Output port for notification persistence
 */
public interface NotificationRepository {
    Mono<Notification> save(Notification notification);
    Flux<Notification> findByOrderId(UUID orderId);
}
