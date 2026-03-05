package com.ecommerce.payment.infrastructure.persistence;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    
    private final R2dbcPaymentRepository r2dbcRepository;
    private final DatabaseClient databaseClient;
    
    @Override
    public Mono<Payment> save(Payment payment) {
        PaymentEntity entity = toEntity(payment);

        return r2dbcRepository.existsById(entity.getId())
            .flatMap(exists -> {
                if (exists) {
                    return r2dbcRepository.save(entity)
                        .map(this::toDomain);
                }

                return insertPayment(entity)
                    .thenReturn(toDomain(entity));
            });
    }
    
    @Override
    public Mono<Payment> findByOrderId(UUID orderId) {
        return r2dbcRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }

    private Mono<Void> insertPayment(PaymentEntity entity) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(
                "INSERT INTO payments (id, order_id, amount, status, payment_method, transaction_id, processed_at, created_at, updated_at) " +
                "VALUES (:id, :orderId, :amount, :status, :paymentMethod, :transactionId, :processedAt, :createdAt, :updatedAt)"
            )
            .bind("id", entity.getId())
            .bind("orderId", entity.getOrderId())
            .bind("amount", entity.getAmount())
            .bind("status", entity.getStatus())
            .bind("createdAt", entity.getCreatedAt())
            .bind("updatedAt", entity.getUpdatedAt());

        spec = entity.getPaymentMethod() != null
            ? spec.bind("paymentMethod", entity.getPaymentMethod())
            : spec.bindNull("paymentMethod", String.class);

        spec = entity.getTransactionId() != null
            ? spec.bind("transactionId", entity.getTransactionId())
            : spec.bindNull("transactionId", String.class);

        spec = entity.getProcessedAt() != null
            ? spec.bind("processedAt", entity.getProcessedAt())
            : spec.bindNull("processedAt", java.time.Instant.class);

        return spec.then();
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
