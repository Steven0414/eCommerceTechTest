package com.ecommerce.order.infrastructure.web;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.application.usecase.*;
import com.ecommerce.order.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * REST adapter (inbound port) - Hexagonal Architecture
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderEventsUseCase getOrderEventsUseCase;
    
    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderCommand command,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.info("Creating order - CorrelationID: {}", correlationId);
        
        return createOrderUseCase.execute(command)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .onErrorResume(IllegalArgumentException.class, 
                e -> Mono.just(ResponseEntity.badRequest().build()))
            .onErrorResume(e -> {
                log.error("Error creating order", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderResponse>> getOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.debug("Getting order {} - CorrelationID: {}", id, correlationId);
        
        return getOrderUseCase.execute(id)
            .map(ResponseEntity::ok)
            .onErrorResume(GetOrderUseCase.OrderNotFoundException.class,
                e -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(e -> {
                log.error("Error getting order", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    @GetMapping
    public Flux<OrderResponse> getOrdersByCustomer(
            @RequestParam UUID customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.debug("Getting orders for customer {} - CorrelationID: {}", customerId, correlationId);
        return getOrderUseCase.executeByCustomer(customerId);
    }

    @PatchMapping("/{id}/confirm")
    public Mono<ResponseEntity<Void>> confirmOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.info("Confirming order {} - CorrelationID: {}", id, correlationId);

        return confirmOrderUseCase.execute(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorResume(ConfirmOrderUseCase.OrderNotFoundException.class,
                e -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(ConfirmOrderUseCase.OrderConfirmationException.class,
                e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()))
            .onErrorResume(e -> {
                log.error("Error confirming order", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    @PatchMapping("/{id}/cancel")
    public Mono<ResponseEntity<Void>> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.info("Cancelling order {} - CorrelationID: {}", id, correlationId);
        
        return cancelOrderUseCase.execute(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorResume(CancelOrderUseCase.OrderNotFoundException.class,
                e -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(CancelOrderUseCase.OrderCancellationException.class,
                e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()))
            .onErrorResume(e -> {
                log.error("Error cancelling order", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    @GetMapping("/{id}/events")
    public Flux<DomainEvent> getOrderEvents(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        log.debug("Getting events for order {} - CorrelationID: {}", id, correlationId);
        return getOrderEventsUseCase.execute(id);
    }
}
