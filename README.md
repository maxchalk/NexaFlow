# NexaFlow — Enterprise-grade Order Management Platform

> Enterprise-grade order management platform built on microservices

## Architecture

```
                    ┌─────────────────────────────────────────────────────┐
                    │              React Frontend (5174)                  │
                    └─────────────────────┬───────────────────────────────┘
                                          │ HTTP
                    ┌─────────────────────▼───────────────────────────────┐
                    │              API Gateway (8080)                     │
                    │         JWT Validation · Rate Limiting · CORS       │
                    └──────┬──────────┬──────────┬──────────┬────────────┘
                           │          │          │          │
              ┌────────────▼──┐ ┌─────▼──┐ ┌────▼───┐ ┌───▼────────────┐
              │ User Service  │ │Product │ │ Order  │ │  Notification  │
              │    (8081)     │ │(8082)  │ │ (8083) │ │   Service(8084)│
              │  PostgreSQL   │ │  PG    │ │   PG   │ │    MongoDB     │
              └──────┬────────┘ └───┬────┘ └───┬────┘ └───────┬────────┘
                     │              │           │               │
                     └──────────────┴─────┬─────┴───────────────┘
                                          │
                    ┌─────────────────────▼───────────────────────────────┐
                    │                    Kafka                            │
                    │  user-events · product-events · order-events        │
                    └─────────────────────────────────────────────────────┘
                                          │
                    ┌─────────────────────▼───────────────────────────────┐
                    │        Observability Stack                          │
                    │  Prometheus (9090) · Grafana (3000) · Zipkin (9411) │
                    └─────────────────────────────────────────────────────┘
```

## Features

- **Microservices architecture** — 4 independent Spring Boot services + API Gateway + Eureka discovery
- **Event-driven communication** — Kafka topics for async inter-service events
- **Circuit breakers** — Resilience4j on every service for fault tolerance
- **Distributed tracing** — Zipkin traces requests across all services
- **Live observability** — Prometheus scrapes metrics every 5s, Grafana dashboards
- **JWT authentication** — Issued by User Service, validated at API Gateway
- **Automated analytics** — Python script generates nightly PDF reports

## Comparison

| Feature | NexaFlow (This) | Datadog | New Relic |
|---------|----------------|---------|-----------|
| Cost | Free / Open Source | $15+/host/mo | $25+/host/mo |
| Self-hosted | ✅ Yes | ❌ No | ❌ No |
| Custom dashboards | ✅ Grafana | ✅ Yes | ✅ Yes |
| Kafka monitoring | ✅ Built-in | ✅ Add-on | ✅ Add-on |
| Source code control | ✅ Full | ❌ Vendor | ❌ Vendor |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Services | Java 21, Spring Boot 3.3.0, Maven |
| Database | PostgreSQL 16 (per-service DB), MongoDB 7 |
| Messaging | Apache Kafka (Confluent 7.5.0) |
| API Gateway | Spring Cloud Gateway 2023.0.1 |
| Service Discovery | Netflix Eureka |
| Observability | Prometheus, Grafana, Zipkin |
| Resilience | Resilience4j (circuit breaker, retry) |
| Security | Spring Security, JJWT 0.12.3 |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Analytics | Python 3.11, pandas, reportlab |
| CI/CD | GitHub Actions |

## How to Run

### Prerequisites
- Docker Desktop running
- Java 21 installed
- Node.js 20 installed
- Python 3.11 installed

### Step 1: Start infrastructure

```bash
cp .env.example .env
# Edit .env with your values
docker-compose up -d
```

Wait for Postgres, MongoDB, Kafka to be healthy.

### Step 2: Create databases

```bash
docker exec nexaflow-postgres psql -U postgres -c "CREATE DATABASE users_db;"
docker exec nexaflow-postgres psql -U postgres -c "CREATE DATABASE products_db;"
docker exec nexaflow-postgres psql -U postgres -c "CREATE DATABASE orders_db;"
```

### Step 3: Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
# Wait until http://localhost:8761 is accessible
```

### Step 4: Start microservices (4 separate terminals)

```bash
# Terminal 1
cd user-service && mvn spring-boot:run

# Terminal 2
cd product-service && mvn spring-boot:run

# Terminal 3
cd order-service && mvn spring-boot:run

# Terminal 4
cd notification-service && mvn spring-boot:run
```

### Step 5: Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

### Step 6: Start Frontend

```bash
cd frontend
npm install
npm run dev
# Opens at http://localhost:5174
```

### Step 7: Run Analytics (optional)

```bash
cd analytics
pip install -r requirements.txt
python report_generator.py
```

## Verify Everything Works

| Service | URL | Expected |
|---------|-----|---------|
| Eureka Dashboard | http://localhost:8761 | Shows all 4 services registered |
| Frontend | http://localhost:5174 | Login/Register page |
| User Service Swagger | http://localhost:8081/swagger-ui.html | API docs |
| Product Service Swagger | http://localhost:8082/swagger-ui.html | API docs |
| Order Service Swagger | http://localhost:8083/swagger-ui.html | API docs |
| Notification Service Swagger | http://localhost:8084/swagger-ui.html | API docs |
| Prometheus | http://localhost:9090 | Metrics scraping |
| Grafana | http://localhost:3000 | Dashboard (admin/admin123) |
| Zipkin | http://localhost:9411 | Trace viewer |

## API Endpoints

### User Service (8081)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/users/auth/register | Public | Register new user |
| POST | /api/users/auth/login | Public | Login, returns JWT |
| GET | /api/users/profile/{id} | Bearer | Get user profile |
| PUT | /api/users/profile/{id} | Bearer | Update profile |

### Product Service (8082)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/products | Public | List all products |
| GET | /api/products/{id} | Public | Get product |
| GET | /api/products/search | Public | Search by keyword/category |
| POST | /api/products | Admin | Create product |
| PATCH | /api/products/{id}/stock | Admin | Update stock |
| DELETE | /api/products/{id} | Admin | Soft delete |

### Order Service (8083)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/orders | Bearer | Place order |
| GET | /api/orders/my | Bearer | My orders |
| GET | /api/orders/{id} | Bearer | Order details |
| PATCH | /api/orders/{id}/cancel | Bearer | Cancel order |
| GET | /api/orders/stats | Admin | Order statistics |

### Notification Service (8084)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/notifications/{userId} | Bearer | Get notifications |
| PATCH | /api/notifications/{id}/read | Bearer | Mark as read |
| GET | /api/notifications/{userId}/unread-count | Bearer | Unread count |

## Grafana Setup

1. Open http://localhost:3000 (admin / admin123)
2. Go to **Configuration → Data Sources → Add data source**
3. Select **Prometheus**, URL: `http://prometheus:9090`
4. Click **Save & Test**
5. Go to **Dashboards → Import**, paste ID `11378` for JVM dashboard

## Connect to Enterprise Patterns

> Built using the same architectural patterns as enterprise healthcare backends:
> event-driven microservices with Kafka, circuit breakers with Resilience4j, and
> centralized observability with Prometheus + Grafana — mirroring patterns used
> at scale in production systems at companies like CVS Health.
