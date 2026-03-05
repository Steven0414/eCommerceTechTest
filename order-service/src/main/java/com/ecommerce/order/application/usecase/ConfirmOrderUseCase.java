package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.outbound.EventPublisher;
import com.ecommerce.order.domain.port.outbound.EventStore;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Command use case for order confirmation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final EventStore eventStore;

    public Mono<Void> execute(UUID orderId) {
        log.info("Confirming order: {}", orderId);

        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found: " + orderId)))
            .flatMap(order -> {
                try {
                    order.changeStatus(OrderStatus.CONFIRMED);
                    return orderRepository.save(order);
                } catch (IllegalStateException e) {
                    return Mono.error(new OrderConfirmationException(e.getMessage()));
                }
            })
            .flatMap(this::publishDomainEvents)
            .then()
            .doOnSuccess(v -> log.info("Order confirmed successfully: {}", orderId))
            .doOnError(error -> log.error("Error confirming order: {}", orderId, error));
    }

    private Mono<Void> publishDomainEvents(com.ecommerce.order.domain.model.Order order) {
        List<DomainEvent> events = order.getDomainEvents();

        return Flux.fromIterable(events)
            .flatMap(event ->
                eventStore.saveEvent(event)
                    .then(eventPublisher.publish(event))
            )
            .then(Mono.fromRunnable(order::clearDomainEvents));
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }

    public static class OrderConfirmationException extends RuntimeException {
        public OrderConfirmationException(String message) {
            super(message);
        }
    }
}
