package com.ecommerce.payment.infrastructure.persistence;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcPaymentRepository extends R2dbcRepository<PaymentEntity, UUID> {
    Mono<PaymentEntity> findByOrderId(UUID orderId);
}
