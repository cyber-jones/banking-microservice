# Banking Microservices — Spring Boot Teaching Project

A complete microservices banking application built to teach Spring Boot from fundamentals to production patterns.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   CLIENT / BROWSER                   │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP :8080
                        ▼
┌─────────────────────────────────────────────────────┐
│              API GATEWAY  (port 8080)                │
│   JWT Validation · Routing · Load Balancing · CORS   │
└───────┬──────────────┬───────────────┬──────────────┘
        │              │               │
        ▼              ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   ACCOUNT    │ │ TRANSACTION  │ │ NOTIFICATION │
│   SERVICE    │ │   SERVICE    │ │   SERVICE    │
│  port: 8081  │ │  port: 8082  │ │  port: 8083  │
│              │ │              │ │              │
│  H2 DB       │ │  H2 DB       │ │  H2 DB       │
│  JWT Auth    │ │  FeignClient │ │  @KafkaListen│
│  REST API    │ │  REST API    │ │  REST API    │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │   Kafka Events  │   Kafka Events  │
       └─────────────────┴────────► Kafka  │
                                    :9092  │
                                           │
┌─────────────────────────────────────────┘
│         EUREKA SERVER  (port 8761)
│         Service Discovery & Registry
└─────────────────────────────────────────
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service discovery registry |
| API Gateway | 8080 | Single entry point, JWT validation, routing |
| Account Service | 8081 | Account CRUD, JWT auth, user management |
| Transaction Service | 8082 | Deposits, withdrawals, transfers via FeignClient |
| Notification Service | 8083 | Kafka consumer, notification persistence |

## Topics Covered

- Spring Boot fundamentals (@SpringBootApplication, auto-configuration, beans)
- REST APIs (@RestController, @GetMapping, ResponseEntity, validation)
- JPA/Hibernate (entities, repositories, JPQL, H2)
- Spring Security (JWT, BCrypt, SecurityFilterChain, @PreAuthorize)
- Microservices (Eureka, API Gateway, FeignClient)
- Kafka (producers, consumers, @KafkaListener, consumer groups)
- Swagger/OpenAPI documentation
- Unit testing (JUnit 5, Mockito, MockMvc, AssertJ)
- Exception handling (@RestControllerAdvice, ProblemDetail)

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for Kafka + Zookeeper)

## Quick Start

### 1. Start Kafka (Docker)
```bash
docker-compose up -d
```

### 2. Start services in order
```bash
# Terminal 1 — Service Discovery
cd eureka-server && mvn spring-boot:run

# Terminal 2 — Account Service
cd account-service && mvn spring-boot:run

# Terminal 3 — Transaction Service
cd transaction-service && mvn spring-boot:run

# Terminal 4 — Notification Service
cd notification-service && mvn spring-boot:run

# Terminal 5 — API Gateway
cd api-gateway && mvn spring-boot:run
```

### 3. Access the apps
| URL | Description |
|-----|-------------|
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8080 | API Gateway (main entry) |
| http://localhost:8081/swagger-ui.html | Account Service Swagger |
| http://localhost:8082/swagger-ui.html | Transaction Service Swagger |
| http://localhost:8083/swagger-ui.html | Notification Service Swagger |
| http://localhost:8081/h2-console | Account Service H2 Console |

## Test the API

### Register + Login
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","email":"test@example.com"}'

# Login — get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'

# Use the token in all further requests:
export TOKEN="<paste token here>"
```

### Account Operations
```bash
# Create account
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Alice","email":"alice@test.com","initialDeposit":1000.00,"accountType":"CHECKING"}'

# Get account
curl http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Transaction Operations
```bash
# Deposit
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC0000000001","amount":500.00,"description":"Salary"}'

# Withdraw
curl -X POST http://localhost:8080/api/v1/transactions/withdraw \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC0000000001","amount":100.00,"description":"ATM"}'

# Transfer
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountNumber":"ACC0000000001","toAccountNumber":"ACC0000000002","amount":200.00}'
```

### Run Tests
```bash
# All tests in a service
cd account-service && mvn test

# Specific test class
mvn test -Dtest=AccountServiceTest

# With coverage report
mvn test jacoco:report
```

## Seeded Credentials
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN, USER |
| john_doe | user123 | USER |
