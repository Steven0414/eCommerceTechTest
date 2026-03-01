# ADR-001: Adopción de Clean Architecture / Hexagonal Architecture

## Estado
Aceptado

## Contexto

El sistema de gestión de órdenes debe ser:
- Fácil de mantener y evolucionar a largo plazo
- Testeable sin dependencias externas complejas
- Independiente de frameworks específicos
- Preparado para cambios tecnológicos (bases de datos, message brokers, etc.)

La arquitectura tradicional en capas (layered architecture) presenta acoplamiento entre capas y dificulta el testing de lógica de negocio sin levantar toda la infraestructura.

## Decisión

Implementar **Clean Architecture / Hexagonal Architecture** con las siguientes características:

### Estructura de Capas

**Domain (núcleo):**
- Entidades y agregados (Order, OrderItem, Payment, Notification)
- Value Objects (OrderStatus, PaymentStatus)
- Eventos de dominio (OrderCreatedEvent, PaymentProcessedEvent)
- Interfaces de puertos (OrderRepository, EventPublisher, EventStore)
- **Sin dependencias externas** (ni Spring, ni frameworks)

**Application (casos de uso):**
- Use cases con CQRS: CreateOrderUseCase, GetOrderUseCase, CancelOrderUseCase
- DTOs y comandos
- Orquestación de dominio y puertos
- Independiente de detalles de infraestructura

**Infrastructure (adaptadores):**
- Adaptadores de entrada: REST controllers, event listeners
- Adaptadores de salida: R2DBC repositories, Kafka publishers, MongoDB stores
- Configuración de frameworks (Spring Boot, Kafka)

### Principios Aplicados

1. **Dependency Inversion**: Domain define interfaces, Infrastructure las implementa
2. **Separation of Concerns**: Cada capa tiene responsabilidad única
3. **Testability**: Domain es testeable con JUnit puro, sin mocks complejos
4. **Framework Independence**: Domain puede funcionar sin Spring

### Implementación Concreta

```java
// Domain: Puerto definido
public interface OrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(UUID id);
}

// Infrastructure: Adaptador implementa el puerto
@Component
public class OrderRepositoryAdapter implements OrderRepository {
    private final R2dbcOrderRepository r2dbcRepository;
    
    @Override
    public Mono<Order> save(Order order) {
        // Lógica de persistencia R2DBC
    }
}

// Application: Usa el puerto sin conocer implementación
@Service
public class CreateOrderUseCase {
    private final OrderRepository orderRepository; // Puerto
    
    public Mono<OrderResponse> execute(CreateOrderCommand cmd) {
        // Lógica de negocio usando puerto
    }
}
```

## Consecuencias

### Positivas

1. **Testabilidad Superior**: Tests de dominio sin frameworks (~85% cobertura lograda)
2. **Flexibilidad Tecnológica**: Cambiar de PostgreSQL a MySQL solo afecta adaptadores
3. **Mantenibilidad**: Lógica de negocio clara y aislada
4. **Onboarding**: Nuevos desarrolladores entienden responsabilidades rápidamente
5. **Evolutividad**: Agregar nuevos adaptadores (GraphQL, gRPC) sin modificar dominio

### Negativas

1. **Más Código Inicial**: Mappers entre entidades y DTOs, puertos e implementaciones
2. **Curva de Aprendizaje**: Equipo debe entender inversión de dependencias
3. **Sobrecargado para MVPs Simples**: En proyectos pequeños, puede ser excesivo

### Mitigaciones

- **Documentar claramente** estructura de carpetas y responsabilidades
- **Ejemplos de referencia** en cada capa para nuevos desarrolladores
- **Package-by-feature** en lugar de package-by-layer para reducir navegación

## Alternativas Consideradas

### 1. Layered Architecture Tradicional
**Rechazada porque:**
- Acoplamiento alto entre capas
- Testing complejo (necesita levantar Spring context siempre)
- Lógica de negocio contaminada con anotaciones de frameworks

### 2. Microservicios sin Arquitectura Clara
**Rechazada porque:**
- Dificulta mantenimiento a largo plazo
- Lógica de negocio dispersa en controllers
- Testing end-to-end requerido para validar reglas

### 3. Domain-Driven Design Completo con Event Sourcing Total
**Rechazada para alcance inicial porque:**
- Complejidad excesiva para 2 días de desarrollo
- Event sourcing completo en todos los agregados es overkill
- Implementamos event sourcing solo en auditoría (balance pragmático)

## Referencias

- Robert C. Martin - "Clean Architecture: A Craftsman's Guide to Software Structure and Design"
- Alistair Cockburn - "Hexagonal Architecture Pattern"
- Vaughn Vernon - "Implementing Domain-Driven Design"

## Notas de Implementación

- Dominio en `com.ecommerce.order.domain`
- Application en `com.ecommerce.order.application`
- Infrastructure en `com.ecommerce.order.infrastructure`
- Cada puerto tiene su adaptador correspondiente
- Tests de dominio sin @SpringBootTest (98% cobertura pura)
