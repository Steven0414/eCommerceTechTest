# ADR-002: Apache Kafka como Message Broker

## Estado
Aceptado

## Contexto

El sistema requiere:
- Comunicación asíncrona entre microservicios
- Desacoplamiento temporal y espacial
- Resiliencia ante fallos temporales de servicios
- Escalabilidad independiente de consumidores
- Auditoría completa de eventos (event sourcing)
- Garantías de orden y entrega

Los microservicios deben poder procesar millones de órdenes durante picos de tráfico (Black Friday, Cyber Monday) sin degradación de servicio.

## Decisión

Seleccionar **Apache Kafka** como message broker principal para comunicación event-driven entre servicios.

### Configuración Implementada

**Tópicos:**
- `order.created`: Publicado por order-service al crear orden
- `order.confirmed`: Publicado cuando orden pasa a CONFIRMED
- `order.cancelled`: Publicado al cancelar orden
- `payment.processed`: Publicado por payment-service cuando pago exitoso
- `payment.failed`: Publicado cuando pago falla

**Productores:**
- `acks=all`: Garantiza que mensaje está replicado
- `retries=3`: Reintentos automáticos en fallos transitorios
- JsonSerializer para payloads

**Consumidores:**
- `enable.auto.commit=false`: Commit manual para garantizar procesamiento
- `auto.offset.reset=earliest`: Procesar todos los eventos desde el inicio
- Consumer groups separados por servicio

### Patrón de Eventos

```java
@Data
public class OrderCreatedEvent implements DomainEvent {
    private String eventId;        // UUID único para idempotencia
    private UUID orderId;
    private UUID customerId;
    private BigDecimal totalAmount;
    private Instant occurredAt;
}
```

### Idempotencia Garantizada

Implementación mediante tabla `processed_events`:

```java
public Mono<Void> processEvent(String eventId, String eventType, 
                               Supplier<Mono<Void>> processor) {
    return checkIfProcessed(eventId)
        .flatMap(processed -> {
            if (processed) {
                return Mono.empty(); // Skip duplicado
            }
            return processor.get()
                .then(markAsProcessed(eventId, eventType));
        });
}
```

**Ventaja**: Reintentos de Kafka no causan duplicación de procesamiento.

## Consecuencias

### Positivas

1. **Desacoplamiento Total**: Servicios no se conocen entre sí
2. **Escalabilidad**: Consumer groups permiten múltiples instancias paralelas
3. **Resiliencia**: Mensajes persisten aunque consumidor esté caído
4. **Auditoría Natural**: Log de eventos sirve como auditoría
5. **Reprocessing**: Poder reprocessar eventos desde offset antiguo
6. **Order Guarantees**: Particiones garantizan orden por aggregate ID (orderId)
7. **High Throughput**: Millones de mensajes/segundo con hardware adecuado

### Negativas

1. **Complejidad Operacional**: Requiere cluster Kafka (Zookeeper, brokers)
2. **Consistencia Eventual**: No strong consistency inmediata
3. **Debugging Más Difícil**: Flujos asíncronos complejos de tracear
4. **Latencia**: Procesamiento asíncrono añade latencia vs síncrono
5. **Curva de Aprendizaje**: Equipo debe entender offsets, partitions, consumer groups

### Mitigaciones

- **Docker Compose** simplifica setup local
- **Correlation ID** en headers HTTP y eventos para trazabilidad
- **Logging estructurado** con JSON para facilitar debugging
- **Health checks** monitorizan conexión a Kafka
- **Dead Letter Queue (DLQ)** planeado para eventos no procesables

## Alternativas Consideradas

### 1. RabbitMQ
**Rechazada porque:**
- Menor throughput que Kafka en escenarios de alto volumen
- No optimizado para event sourcing (logs inmutables)
- Push model puede saturar consumidores lentos

**Ventajas que tiene:** 
- Más fácil de operar
- Mejor para routing complejo con exchanges

### 2. Amazon SQS/SNS
**Rechazada porque:**
- Vendor lock-in (AWS)
- No disponible en deployment on-premise
- Más caro en volúmenes altos

**Ventajas que tiene:**
- Fully managed, sin operaciones
- Scaling automático

### 3. REST Síncrono con Circuit Breaker
**Rechazada porque:**
- Acoplamiento temporal (servicios deben estar UP)
- No permite reprocessing de eventos
- Dificulta escalado independiente
- Sin auditoría natural

### 4. Event Store + Pub/Sub Simple
**Rechazada porque:**
- Reinventar la rueda (Kafka ya resuelve esto)
- Desvía esfuerzo de lógica de negocio

## Estrategia de Implementación

### Fase 1: Setup Básico ✅
- Tópicos creados automáticamente
- Producers en order-service
- Consumers en payment-service y notification-service
- Idempotencia básica

### Fase 2: Hardening (Siguiente Sprint)
- Dead Letter Queue para mensajes fallidos
- Retry con backoff exponencial
- Monitoring con JMX metrics
- Alerting en lag de consumer groups

### Fase 3: Optimizaciones (Futuro)
- Particionamiento por customerId para paralelismo
- Kafka Streams para agregaciones
- Schema Registry con Avro para contratos de eventos
- Compactación de tópicos para snapshots

## Métricas de Éxito

- **Latencia P99 < 500ms** en procesamiento de eventos
- **Uptime 99.9%** de consumers (máximo 8.7h downtime/año)
- **Zero duplicados procesados** gracias a idempotencia
- **Throughput 10,000 órdenes/min** en picos

## Referencias

- "Designing Data-Intensive Applications" - Martin Kleppmann (Capítulo 11)
- "Building Event-Driven Microservices" - Adam Bellemare
- Documentación oficial de Kafka: https://kafka.apache.org/documentation/

## Notas de Implementación

```yaml
# docker-compose.yml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1  # 3 en producción
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
```

**Advertencia Producción**: 
- Replication factor mínimo 3 para durabilidad
- Min.insync.replicas 2 para evitar pérdida de datos
- Monitorear lag de consumer groups constantemente
