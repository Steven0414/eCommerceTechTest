package com.ecommerce.payment.application.service;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/**
 * Payment processing service with simulated gateway
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final Random random = new Random();
    
    @Value("${app.payment.success-rate:0.8}")
    private double successRate;
    
    @Value("${app.payment.processing-delay-ms:2000}")
    private long processingDelayMs;
    
    public Mono<Payment> processPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing payment for order: {}", orderId);
        
        Payment payment = Payment.create(orderId, amount);
        
        return paymentRepository.save(payment)
            .flatMap(savedPayment -> simulatePaymentGateway()
                .flatMap(result -> {
                    String transactionId = UUID.randomUUID().toString();
                    savedPayment.process(result, transactionId);
                    return paymentRepository.save(savedPayment);
                })
            )
            .doOnSuccess(result -> 
                log.info("Payment {} for order {}", 
                    result.getStatus(), result.getOrderId())
            );
    }
    
    public Mono<Payment> getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    public Mono<Payment> retryPayment(UUID orderId) {
        log.info("Retrying payment for order: {}", orderId);
        
        return paymentRepository.findByOrderId(orderId)
            .flatMap(payment -> {
                payment.setStatus(com.ecommerce.payment.domain.model.PaymentStatus.PROCESSING);
                payment.setUpdatedAt(java.time.Instant.now());
                return paymentRepository.save(payment);
            })
            .flatMap(payment -> simulatePaymentGateway()
                .flatMap(result -> {
                    String transactionId = UUID.randomUUID().toString();
                    payment.process(result, transactionId);
                    return paymentRepository.save(payment);
                })
            );
    }
    
    private Mono<Boolean> simulatePaymentGateway() {
        return Mono.delay(Duration.ofMillis(processingDelayMs))
            .map(tick -> random.nextDouble() < successRate);
    }
}
