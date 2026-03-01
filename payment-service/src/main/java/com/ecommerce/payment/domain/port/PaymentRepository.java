package com.ecommerce.payment.domain.port;

import com.ecommerce.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository {
    Mono<Payment> save(Payment payment);
    Mono<Payment> findByOrderId(UUID orderId);
}
