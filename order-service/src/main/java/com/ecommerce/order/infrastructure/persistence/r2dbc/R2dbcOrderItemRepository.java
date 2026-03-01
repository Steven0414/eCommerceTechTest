package com.ecommerce.order.infrastructure.persistence.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface R2dbcOrderItemRepository extends R2dbcRepository<OrderItemEntity, UUID> {
    Flux<OrderItemEntity> findByOrderId(UUID orderId);
}
