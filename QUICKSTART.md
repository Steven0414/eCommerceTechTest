# Quick Start Guide

## 🚀 Inicio Rápido (5 minutos)

### Prerequisitos
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 8GB RAM disponible

### Steps

1. **Build services**
```bash
./build.sh
```

2. **Start infrastructure**
```bash
docker-compose up --build
```

3. **Wait for health checks** (~2 minutos)
```bash
# Verificar servicios
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health  
curl http://localhost:8082/actuator/health
```

4. **Create a test order**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "items": [{
      "productId": "223e4567-e89b-12d3-a456-426614174111",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 999.99
    }]
  }'
```

5. **Check order status**
```bash
# Usar el orderId retornado en el paso anterior
curl http://localhost:8080/api/v1/orders/{orderId}
```

6. **View event history (Event Sourcing)**
```bash
curl http://localhost:8080/api/v1/orders/{orderId}/events
```

7. **Check notifications**
```bash
curl http://localhost:8082/api/v1/notifications?orderId={orderId}
```

## 🧪 Run Tests

```bash
cd order-service && mvn test
cd payment-service && mvn test
cd notification-service && mvn test
```

## 🛑 Stop Everything

```bash
docker-compose down -v
```

## 📖 Full Documentation

See [README.md](README.md) for complete documentation.
