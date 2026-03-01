# ADR-003: Estrategia de Manejo de Errores y Compensaciones en Sagas

## Estado
Aceptado

## Contexto

El flujo de procesamiento de órdenes involucra múltiples servicios y operaciones:

```
Order Created → Inventory Validated → Payment Processed → 
Order Confirmed → Notification Sent → Inventory Updated
```

Cada paso puede fallar por razones técnicas o de negocio:
- Pago rechazado por gateway
- Inventario insuficiente
- Servicio de notificación caído
- Timeout en llamadas externas

**Problema**: Sin transacciones ACID distribuidas, ¿cómo garantizar consistencia?

**Restricciones**:
- No usar 2PC (Two-Phase Commit) - no escalable
- Consistencia eventual es aceptable
- Usuario debe saber resultado final de su orden

## Decisión

Implementar **Saga Pattern con orquestación basada en eventos** y estrategia de compensación explícita.

### Enfoque Seleccionado: Saga Coreografiada

Cada servicio:
1. Escucha evento que le corresponde
2. Realiza su operación
3. Publica evento de éxito o fallo
4. Siguiente servicio reacciona al evento

**No hay orquestador central** - servicios coordinan mediante eventos.

### Manejo de Errores Implementado

#### Nivel 1: Reintentos con Backpressure

```java
public Mono<Payment> processPayment(UUID orderId, BigDecimal amount) {
    return paymentGateway.charge(amount)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
            .filter(ex -> ex instanceof TransientException))
        .timeout(Duration.ofSeconds(10))
        .onErrorResume(TimeoutException.class, 
            ex -> publishPaymentFailed(orderId, "TIMEOUT"));
}
```

**Aplicado a**: Fallos transitorios (conexión, timeout)

#### Nivel 2: Dead Letter Queue (Planeado)

Eventos que fallan después de N reintentos:
- Van a tópico `payment.failed.dlq`
- Dashboard muestra fallos para intervención manual
- Operator puede reintentarlo o cancelar la orden

#### Nivel 3: Compensaciones de Negocio

**Escenario**: Pago exitoso pero notificación falla

```java
// Payment Service publica
PaymentProcessedEvent → order.status = PAID

// Si Notification falla 5 veces:
NotificationFailedEvent → trigger manual review
// Usuario recibe notificación eventualmente (retry separado)
```

**Decisión**: No revertir el pago - notificación failure no es crítico para negocio.

**Escenario 2**: Inventario insuficiente después de crear orden

```java
// Inventory Service detecta problema
InventoryInsufficientEvent → order-service

// Order Service compensa
order.changeStatus(CANCELLED)
publishOrderCancelledEvent()
```

**Resultado**: Usuario ve orden CANCELLED, frontend muestra mensaje apropiado.

### Estados de Error Definidos

```java
public enum OrderStatus {
    PENDING,      // Inicial
    CONFIRMED,    // Inventario OK
    PAYMENT_PROCESSING,
    PAID,         // Pago exitoso
    FAILED,       // Error irrecuperable
    CANCELLED     // Cancelado por usuario o compensación
}
```

**FAILED** se usa cuando:
- Pago rechazado definitivamente (tarjeta inválida)
- Timeout después de reintentos agotados
- Error de validación de negocio

### Idempotencia para Evitar Duplicados

```java
@KafkaListener(topics = "order.confirmed")
public void handleOrderConfirmed(String eventId, String payload) {
    idempotencyService.processEvent(eventId, () -> {
        // Procesar pago
        return paymentService.process(orderId, amount)
            .flatMap(this::publishResult);
    }).subscribe();
}
```

**Garantía**: Mismo evento procesado N veces no causa N cargos.

## Consecuencias

### Positivas

1. **Resiliencia**: Fallo de un servicio no colapsa toda la transacción
2. **Escalabilidad**: No hay coordinador que sea single point of failure
3. **Auditoría**: Todos los pasos quedan en event store
4. **Debugging**: Event log muestra exactamente qué pasó
5. **Flexibilidad**: Agregar nuevo paso no requiere cambiar orquestador

### Negativas

1. **Complejidad**: Difícil visualizar flujo completo
2. **Testing**: Tests end-to-end complejos (múltiples servicios)
3. **Debugging en Producción**: Tracear fallo requiere correlation ID
4. **Latencia**: Cada hop asíncrono añade latencia
5. **Compensación Manual**: Algunos casos requieren intervención humana

### Mitigaciones

- **Correlation ID** en todos los eventos para trazabilidad
- **Dashboards** mostrando estado de sagas en progreso
- **Timeouts agresivos** para detectar fallos rápido
- **Circuit breaker** (planeado para Fase 2) en llamadas externas
- **Alerting** en DLQ con tamaño > threshold

## Alternativas Consideradas

### 1. Saga Orquestada con Orchestrator Central
**Rechazada porque:**
- Orchestrator es single point of failure
- Acoplamiento: todos los servicios deben hablar con orchestrator
- Menos flexible para evolucionar flujo

**Ventaja que tiene:**
- Más fácil visualizar flujo completo
- Debugging centralizado

### 2. Distributed Transactions (2PC)
**Rechazada porque:**
- No escala en microservicios
- Bloquea recursos durante coordinación
- Kafka no soporta 2PC nativamente

### 3. No Hacer Nada (Best Effort)
**Rechazada porque:**
- Usuario puede ser cargado sin recibir producto
- Pérdida de confianza en plataforma

### 4. Compensación Transaccional Completa
**Rechazada para MVP porque:**
- Complejidad > valor agregado (pocas compensaciones necesarias)
- Ejemplo: reversar pago tiene costo financiero alto
- Estrategia pragmática: notificar usuario + intervención manual

## Estrategia de Testing

### Unit Tests
```java
@Test
void shouldCompensateOrderWhenPaymentFails() {
    // Given: Order en PAYMENT_PROCESSING
    // When: PaymentFailedEvent recibido
    // Then: Order pasa a FAILED
}
```

### Integration Tests con Testcontainers
```java
@Test
void shouldCompleteFullSagaSuccessfully() {
    // Given: Infraestructura levantada (Kafka, DBs)
    // When: POST /orders
    // Then: Esperar eventos secuencialmente
    // Verify: Order PAID, Payment APPROVED, Notification SENT
}
```

### Chaos Engineering (Futuro)
- Matar payment-service durante procesamiento
- Validar que: orden pasa a FAILED después de timeout
- Validar: reintento manual funciona

## Métricas de Éxito

- **Saga Success Rate > 95%** en happy path
- **Mean Time to Recovery < 5 min** en fallos
- **Zero inconsistencias** detectadas en auditorías mensuales
- **Compensation Rate < 2%** de todas las transacciones

## Plan de Evolución

### Fase 1: MVP (Actual) ✅
- Saga básica: OrderCreated → PaymentProcessed
- Compensación simple: PaymentFailed → OrderFailed
- Idempotencia garantizada

### Fase 2: Hardening
- Dead Letter Queue operacional
- Dashboard de sagas en progreso
- Circuit breaker en payment gateway
- Retry con exponential backoff

### Fase 3: Avanzado
- Saga orchestrator para flujos complejos
- Compensación transaccional automática (reversar inventario)
- Chaos testing automatizado
- SLA monitoring por paso de saga

## Referencias

- "Microservices Patterns" - Chris Richardson (Capítulo 4: Sagas)
- "Enterprise Integration Patterns" - Hohpe & Woolf
- Netflix Tech Blog: "Distributed Transactions at Scale"

## Notas de Implementación

```java
// payment-service/OrderEventConsumer.java
@KafkaListener(topics = "order.confirmed")
public void handleOrderConfirmed(String eventId, String message) {
    idempotencyService.processEvent(eventId, "OrderConfirmed", () ->
        processOrderConfirmedEvent(message, eventId)
            .then(markAsProcessed(eventId))
    ).subscribe();
}
```

**Advertencia**: Siempre verificar idempotencia ANTES de procesar operación costosa (cargo a tarjeta).
