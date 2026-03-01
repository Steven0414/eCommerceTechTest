package com.ecommerce.payment.application.service;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        
        // When & Then
        StepVerifier.create(paymentService.processPayment(orderId, amount))
            .expectNextMatches(payment -> 
                payment.getOrderId().equals(orderId) &&
                payment.getAmount().compareTo(amount) == 0 &&
                (payment.getStatus() == PaymentStatus.APPROVED || 
                 payment.getStatus() == PaymentStatus.FAILED)
            )
            .verifyComplete();
    }
    
    @Test
    void shouldGetPaymentByOrderId() {
        // Given
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .orderId(orderId)
            .amount(new BigDecimal("100.00"))
            .status(PaymentStatus.APPROVED)
            .build();
        
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Mono.just(payment));
        
        // When & Then
        StepVerifier.create(paymentService.getPaymentByOrderId(orderId))
            .expectNext(payment)
            .verifyComplete();
    }
}
