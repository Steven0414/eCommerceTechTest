package com.ecommerce.order.infrastructure.web;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");
    
    @Autowired
    private WebTestClient webTestClient;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> 
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(UUID.randomUUID())
            .items(List.of(
                CreateOrderCommand.OrderItemDto.builder()
                    .productId(UUID.randomUUID())
                    .productName("Test Product")
                    .quantity(2)
                    .unitPrice(new BigDecimal("10.00"))
                    .build()
            ))
            .build();
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(command)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .value(response -> {
                assertThat(response.getId()).isNotNull();
                assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            });
    }
    
    @Test
    void shouldGetOrderById() {
        // Given - Create order first
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(UUID.randomUUID())
            .items(List.of(createTestItemDto()))
            .build();
        
        OrderResponse createdOrder = webTestClient.post()
            .uri("/api/v1/orders")
            .bodyValue(command)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .returnResult()
            .getResponseBody();
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders/" + createdOrder.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderResponse.class)
            .value(response -> {
                assertThat(response.getId()).isEqualTo(createdOrder.getId());
                assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            });
    }
    
    @Test
    void shouldCancelOrder() {
        // Given - Create order first
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(UUID.randomUUID())
            .items(List.of(createTestItemDto()))
            .build();
        
        OrderResponse createdOrder = webTestClient.post()
            .uri("/api/v1/orders")
            .bodyValue(command)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .returnResult()
            .getResponseBody();
        
        // When & Then
        webTestClient.patch()
            .uri("/api/v1/orders/" + createdOrder.getId() + "/cancel")
            .exchange()
            .expectStatus().isNoContent();
        
        // Verify status changed
        webTestClient.get()
            .uri("/api/v1/orders/" + createdOrder.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderResponse.class)
            .value(response -> 
                assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED)
            );
    }
    
    @Test
    void shouldGetOrdersByCustomerId() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(customerId)
            .items(List.of(createTestItemDto()))
            .build();
        
        webTestClient.post()
            .uri("/api/v1/orders")
            .bodyValue(command)
            .exchange()
            .expectStatus().isCreated();
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders?customerId=" + customerId)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(OrderResponse.class)
            .value(orders -> {
                assertThat(orders).isNotEmpty();
                assertThat(orders.get(0).getCustomerId()).isEqualTo(customerId);
            });
    }
    
    @Test
    void shouldReturnNotFoundForNonExistentOrder() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders/" + UUID.randomUUID())
            .exchange()
            .expectStatus().isNotFound();
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
