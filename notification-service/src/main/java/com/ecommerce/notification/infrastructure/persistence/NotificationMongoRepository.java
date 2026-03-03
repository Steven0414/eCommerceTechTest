package com.ecommerce.notification.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface NotificationMongoRepository extends ReactiveMongoRepository<NotificationDocument, String> {
    Flux<NotificationDocument> findByOrderId(UUID orderId);
}
