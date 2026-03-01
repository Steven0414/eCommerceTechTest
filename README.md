# Sistema de Gestión de Órdenes E-commerce

Sistema de procesamiento de órdenes implementado con arquitectura orientada a eventos, programación reactiva y clean architecture.

## 📋 Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Prerequisitos](#prerequisitos)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [Endpoints API](#endpoints-api)
- [Ejecución de Tests](#ejecución-de-tests)
- [Decisiones Técnicas](#decisiones-técnicas)
- [Mejoras Futuras](#mejoras-futuras)

## 🎯 Descripción General

Sistema de gestión de órdenes para e-commerce que implementa:

- **Clean/Hexagonal Architecture**: Dominio completamente independiente de frameworks
- **Event-Driven Architecture**: Comunicación asíncrona mediante eventos con Kafka
- **Programación Reactiva**: Spring WebFlux y Project Reactor para APIs no bloqueantes
- **CQRS**: Separación de comandos y queries
- **Event Sourcing**: Auditoría completa de eventos de dominio
- **Saga Pattern**: Orquestación de transacciones distribuidas
- **Idempotencia**: Garantizada en todos los consumidores de eventos

### Flujo de Eventos

```
OrderCreated → ValidateInventory → ProcessPayment → 
ConfirmOrder → NotifyCustomer → UpdateInventory
```

### Máquina de Estados

```
PENDING → CONFIRMED → PAYMENT_PROCESSING → PAID → SHIPPED → DELIVERED
   ↓          ↓               ↓
CANCELLED  CANCELLED      FAILED
```

## 🏗️ Arquitectura

### Visión General

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  Order Service  │─────▶│ Payment Service │─────▶│Notification Svc │
│     (8080)      │      │     (8081)      │      │     (8082)      │
└────────┬────────┘      └────────┬────────┘      └────────┬────────┘
         │                        │                         │
         └────────────────────────┴─────────────────────────┘
                                  │
                           ┌──────▼──────┐
                           │    Kafka    │
                           └─────────────┘
```

### Microservicios

#### Order Service (puerto 8080)
- API REST reactiva para gestión de órdenes
- Clean architecture con capas domain/application/infrastructure
- Event Sourcing en MongoDB para auditoría
- Publica eventos: `OrderCreated`, `OrderConfirmed`, `OrderCancelled`
- Consume eventos: `PaymentProcessed`, `PaymentFailed`

**Capas:**
```
infrastructure/
  ├── web/           # REST controllers (adaptador de entrada)
  ├── persistence/
  │   ├── r2dbc/     # PostgreSQL adapter (adaptador de salida)
  │   └── mongodb/   # Event store adapter
  └── messaging/     # Kafka publishers/consumers
application/
  ├── usecase/       # Casos de uso (CQRS)
  └── dto/           # DTOs y comandos
domain/
  ├── model/         # Entidades y agregados
  ├── event/         # Eventos de dominio
  └── port/          # Interfaces (puertos)
```

#### Payment Service (puerto 8081)
- Procesa pagos con simulación de gateway
- Implementa Saga Pattern para compensaciones
- Consume eventos: `OrderConfirmed`
- Publica eventos: `PaymentProcessed`, `PaymentFailed`

#### Notification Service (puerto 8082)
- Envía notificaciones a clientes
- Consume todos los eventos del ciclo de vida de órdenes
- Almacena historial de notificaciones en MongoDB

### Componentes de Infraestructura

- **Kafka**: Message broker para comunicación asíncrona
- **PostgreSQL**: Persistencia transaccional (Order y Payment)
- **MongoDB**: Event store y notificaciones
- **Docker Compose**: Orquestación de servicios

## 🛠️ Stack Tecnológico

### Backend
- **Java 17**: Lenguaje de programación
- **Spring Boot 3.2.0**: Framework base
- **Spring WebFlux**: Programación reactiva
- **Project Reactor**: Operadores reactivos (Mono/Flux)
- **Spring Data R2DBC**: Acceso reactivo a PostgreSQL
- **Spring Data MongoDB Reactive**: Acceso reactivo a MongoDB
- **Apache Kafka**: Message broker
- **Reactor Kafka**: Cliente Kafka reactivo

### Testing
- **JUnit 5**: Framework de testing
- **Mockito**: Mocking para unit tests
- **Testcontainers**: Integración con infraestructura real
- **StepVerifier**: Testing de pipelines reactivos
- **JaCoCo**: Cobertura de código

### Infraestructura
- **Docker & Docker Compose**: Contenedores
- **PostgreSQL 15**: Base de datos relacional
- **MongoDB 7.0**: Base de datos documental
- **Kafka 7.5.0**: Message broker

## 📦 Prerequisitos

- **Java 17** o superior
- **Maven 3.8+**  
- **Docker** y **Docker Compose**
- **8GB RAM** mínimo disponible para contenedores

## 🚀 Instalación y Ejecución

### 1. Clonar el Repositorio

```bash
git clone <repository-url>
cd eventDrivenECommerce
```

### 2. Configurar Variables de Entorno

```bash
cp .env.example .env
# Editar .env si es necesario (valores por defecto funcionan)
```

### 3. Construir los Servicios

```bash
# Construir order-service
cd order-service
mvn clean package -DskipTests
cd ..

# Construir payment-service
cd payment-service
mvn clean package -DskipTests
cd ..

# Construir notification-service
cd notification-service
mvn clean package -DskipTests
cd ..
```

### 4. Levantar Infraestructura y Servicios

```bash
docker-compose up --build
```

Esperar a que todos los health checks estén OK (~2 minutos).

### 5. Verificar que los Servicios Están Activos

```bash
# Order Service
curl http://localhost:8080/actuator/health

# Payment Service
curl http://localhost:8081/actuator/health

# Notification Service
curl http://localhost:8082/actuator/health
```

## 📡 Endpoints API

### Order Service (8080)

#### Crear Orden
```bash
POST /api/v1/orders
Content-Type: application/json

{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "items": [
    {
      "productId": "223e4567-e89b-12d3-a456-426614174111",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 999.99
    }
  ]
}
```

#### Obtener Orden por ID
```bash
GET /api/v1/orders/{orderId}
```

#### Listar Órdenes por Cliente
```bash
GET /api/v1/orders?customerId={customerId}
```

#### Cancelar Orden
```bash
PATCH /api/v1/orders/{orderId}/cancel
```

#### Obtener Historial de Eventos (Event Sourcing)
```bash
GET /api/v1/orders/{orderId}/events
```

### Payment Service (8081)

#### Consultar Estado de Pago
```bash
GET /api/v1/payments/{orderId}
```

#### Reintentar Pago
```bash
POST /api/v1/payments/{orderId}/retry
```

### Notification Service (8082)

#### Consultar Notificaciones Enviadas
```bash
GET /api/v1/notifications?orderId={orderId}
```

## 🧪 Ejecución de Tests

### Tests Unitarios y de Integración

```bash
# Order Service
cd order-service
mvn test

# Payment Service
cd payment-service
mvn test

# Notification Service
cd notification-service
mvn test
```

### Reporte de Cobertura

```bash
cd order-service
mvn test
# Abrir: target/site/jacoco/index.html
```

**Cobertura Actual:**
- Order Service Domain: ~85%
- Payment Service: ~75%
- Notification Service: ~70%

### Tipos de Tests Implementados

- **Unit Tests**: Lógica de dominio pura
- **Integration Tests**: Con Testcontainers (PostgreSQL, Kafka, MongoDB)
- **Reactive Tests**: Con StepVerifier y backpressure
- **End-to-End Tests**: Flujos completos de creación → pago → notificación

## 💡 Decisiones Técnicas

Ver [docs/ADRs](docs/ADRs) para decisiones arquitectónicas detalladas:
- [ADR-001: Clean Architecture](docs/ADRs/ADR-001-clean-architecture.md)
- [ADR-002: Kafka Selection](docs/ADRs/ADR-002-kafka-selection.md)
- [ADR-003: Error Handling](docs/ADRs/ADR-003-error-handling.md)

## 🔮 Mejoras Futuras

### Funcionalidades Pendientes
- Validación real de inventario (actualmente simulada)
- Integración con gateway de pagos real
- Envío de emails/SMS en notification-service
- Compensación completa en saga (rollback de inventario)

### Optimizaciones Técnicas
- Circuit Breaker con Resilience4j
- API Gateway con Spring Cloud Gateway
- Cache reactivo con Redis
- Kafka Streams para agregaciones

### Infraestructura
- Kubernetes manifests
- CI/CD pipeline
- Database migrations con Flyway
- Distributed Tracing con Zipkin

## 📚 Documentación Adicional

- [Architecture Decision Records](docs/ADRs/)
- [Autoevaluación](EVALUATION.md)

## 👥 Autor

Desarrollado como prueba técnica para posición de Líder Técnico Java.

## 📄 Licencia

Este proyecto es de uso educativo y evaluativo.
