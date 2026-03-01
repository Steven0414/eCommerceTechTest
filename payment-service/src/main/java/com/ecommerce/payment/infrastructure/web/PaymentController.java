package com.ecommerce.payment.infrastructure.web;

import com.ecommerce.payment.application.service.PaymentService;
import com.ecommerce.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Payment>> getPaymentByOrderId(@PathVariable UUID orderId) {
        log.debug("Getting payment for order: {}", orderId);
        
        return paymentService.getPaymentByOrderId(orderId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{orderId}/retry")
    public Mono<ResponseEntity<Payment>> retryPayment(@PathVariable UUID orderId) {
        log.info("Retrying payment for order: {}", orderId);
        
        return paymentService.retryPayment(orderId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
