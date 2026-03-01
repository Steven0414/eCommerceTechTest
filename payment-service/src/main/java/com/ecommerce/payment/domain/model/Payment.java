package com.ecommerce.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String transactionId;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static Payment create(UUID orderId, BigDecimal amount) {
        Instant now = Instant.now();
        return Payment.builder()
            .id(UUID.randomUUID())
            .orderId(orderId)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
    
    public void process(boolean success, String transactionId) {
        this.status = success ? PaymentStatus.APPROVED : PaymentStatus.FAILED;
        this.transactionId = transactionId;
        this.processedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
