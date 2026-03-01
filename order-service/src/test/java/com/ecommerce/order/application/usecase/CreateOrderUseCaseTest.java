package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.port.outbound.EventPublisher;
import com.ecommerce.order.domain.port.outbound.EventStore;
import com.ecommerce.order.domain.port.outbound.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateOrderUseCase with reactive testing
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private EventStore eventStore;
    
    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(customerId)
            .items(List.of(createTestItemDto()))
            .build();
        
        when(orderRepository.save(any(Order.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(eventStore.saveEvent(any())).thenReturn(Mono.empty());
        when(eventPublisher.publish(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(createOrderUseCase.execute(command))
            .expectNextMatches(response -> 
                response.getCustomerId().equals(customerId) &&
                response.getStatus() == OrderStatus.PENDING &&
                response.getTotalAmount().compareTo(new BigDecimal("20.00")) == 0
            )
            .verifyComplete();
        
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publish(any());
    }
    
    @Test
    void shouldHandleValidationErrorOnInvalidQuantity() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(UUID.randomUUID())
            .items(List.of(
                CreateOrderCommand.OrderItemDto.builder()
                    .productId(UUID.randomUUID())
                    .productName("Product")
                    .quantity(0)
                    .unitPrice(new BigDecimal("10.00"))
                    .build()
            ))
            .build();
        
        // When & Then
        StepVerifier.create(createOrderUseCase.execute(command))
            .expectError(IllegalArgumentException.class)
            .verify();
        
        verify(orderRepository, never()).save(any());
    }
    
    private CreateOrderCommand.OrderItemDto createTestItemDto() {
        return CreateOrderCommand.OrderItemDto.builder()
            .productId(UUID.randomUUID())
            .productName("Test Product")
            .quantity(2)
            .unitPrice(new BigDecimal("10.00"))
            .build();
    }
}
