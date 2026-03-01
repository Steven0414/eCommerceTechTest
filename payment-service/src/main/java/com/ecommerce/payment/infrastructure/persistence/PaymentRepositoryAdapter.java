package com.ecommerce.payment.infrastructure.persistence;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    
    private final R2dbcPaymentRepository r2dbcRepository;
    
    @Override
    public Mono<Payment> save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        return r2dbcRepository.save(entity)
            .map(this::toDomain);
    }
    
    @Override
    public Mono<Payment> findByOrderId(UUID orderId) {
        return r2dbcRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }
    
    private PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .status(payment.getStatus().name())
            .paymentMethod(payment.getPaymentMethod())
            .transactionId(payment.getTransactionId())
            .processedAt(payment.getProcessedAt())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }
    
    private Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
            .id(entity.getId())
            .orderId(entity.getOrderId())
            .amount(entity.getAmount())
            .status(PaymentStatus.valueOf(entity.getStatus()))
            .paymentMethod(entity.getPaymentMethod())
            .transactionId(entity.getTransactionId())
            .processedAt(entity.getProcessedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
