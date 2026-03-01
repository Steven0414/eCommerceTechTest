package com.ecommerce.order.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface MongoEventRepository extends ReactiveMongoRepository<EventDocument, String> {
    Flux<EventDocument> findByAggregateIdOrderByOccurredAt(UUID aggregateId);
}
