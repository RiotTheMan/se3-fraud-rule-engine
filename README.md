[![CI](https://github.com/RiotTheMan/se3-fraud-rule-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/RiotTheMan/se3-fraud-rule-engine/actions/workflows/ci.yml)

# Fraud Rule Engine

Event-driven fraud detection service. Consumes categorised transaction events from Kafka, evaluates them against six configurable fraud rules, persists fraud flags in PostgreSQL, and exposes a JWT-secured REST API for analysts.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Messaging | Apache Kafka (Confluent 7.5.3) |
| Persistence | PostgreSQL 15, Spring Data JPA, Flyway |
| Security | Spring Security OAuth2 Resource Server (JWT) |
| Mapping | MapStruct |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, EmbeddedKafka |
| Build | Maven 3, GitHub Actions |
| Runtime | Java 21 virtual threads (`newVirtualThreadPerTaskExecutor`) |

---

## Project structure

```
fraud-rule-engine/
├── src/main/java/.../fraudengine/
│   ├── config/             JwtSecurityConfiguration, KafkaConfig, SecurityConfig
│   ├── domain/             Customer, Transaction, FraudRule, FraudFlag
│   ├── engine/             Rule engine core
│   │   ├── FraudRuleStrategy.java          Strategy interface
│   │   ├── RuleEvaluationPipeline.java     Orchestrator — full eval, virtual threads
│   │   ├── RuleRegistry.java               DB-backed cache, volatile reference swap
│   │   ├── FraudRuleFactory.java           Static factory for test construction
│   │   ├── EvaluationContext.java          Immutable per-transaction context
│   │   └── rules/
│   │       ├── VelocityRule.java           10 transactions / 60 min
│   │       ├── LargeAmountRule.java        R50k / $3k / €2.8k threshold
│   │       ├── UnusualHourRule.java        02:00–06:00 SAST window
│   │       ├── DuplicateTransactionRule.java  Same merchant+amount within 300s
│   │       ├── GeographicAnomalyRule.java  Haversine + 30-day country history
│   │       └── CategoryMismatchRule.java   Two-tier: high-risk list + 90-day profile
│   ├── event/              Kafka consumer + TransactionCategorizedEvent DTO
│   ├── errors/             GlobalExceptionHandler
│   ├── repository/         Spring Data JPA repositories
│   ├── service/            FraudFlagService, TransactionIngestionService
│   └── web/                Controllers, DTOs, MapStruct mappers
├── src/main/resources/
│   ├── application.yml                     Thin base config (external secrets import)
│   ├── application-local.yml.example       Local dev template — copy to application-local.yml
│   ├── local-jwt-public.pem                RSA public key for local JWT validation
│   └── db/migration/
│       ├── V1__initial_schema.sql          All tables + indexes with documented rationale
│       └── V2__seed_fraud_rules.sql        Initial rule configurations
├── src/test/               45 tests — unit, slice, integration
├── .github/workflows/ci.yml
├── docker-compose.yml      Full stack: Zookeeper + Kafka + PostgreSQL + app
└── Dockerfile
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Docker + Docker Compose | any recent |

---

## Running with Docker Compose

The fastest way to run the full stack. Starts Zookeeper, Kafka, PostgreSQL, and the application together.

**Step 1 — Build the JAR**

```bash
mvn clean package -DskipTests
```

**Step 2 — Start the stack**

```bash
docker compose up --build
```

On first run Flyway automatically creates all tables and seeds the six fraud rules.

| Service | URL |
|---|---|
| Application | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Kafka | localhost:9092 |
| PostgreSQL | localhost:5434 |

**Stop**

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

---

## Running locally (without Docker)

**Step 1 — Start infrastructure**

You still need Kafka and PostgreSQL. Start just the infrastructure containers:

```bash
docker compose up zookeeper kafka postgres -d
```

**Step 2 — Configure the application**

Copy the example config and fill in your values:

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

The example ships with defaults that match the Docker Compose infrastructure — for a standard local setup no changes are needed.

**Step 3 — Run**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts on port 8080. Flyway migrations run automatically on startup.

---

## Running tests

```bash
mvn test
```

The test suite uses `@EmbeddedKafka` and Testcontainers (PostgreSQL) — no external infrastructure required.

**Testcontainers on Rancher Desktop** requires the following environment (already wired into `pom.xml`):

```
DOCKER_HOST=unix://${user.home}/.rd/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=${user.home}/.rd/docker.sock
TESTCONTAINERS_RYUK_DISABLED=true
```

| Test class | Type | Count |
|---|---|---|
| VelocityRuleTest | Unit | 5 |
| LargeAmountRuleTest | Unit | 5 |
| UnusualHourRuleTest | Unit | 7 |
| DuplicateTransactionRuleTest | Unit | 5 |
| GeographicAnomalyRuleTest | Unit | 5 |
| CategoryMismatchRuleTest | Unit | 6 |
| RuleEvaluationPipelineTest | Unit | 5 |
| FraudFlagControllerTest | Slice (MockMvc) | 5 |
| TransactionIngestionIntegrationTest | Integration | 2 |
| **Total** | | **45 / 45** |

---

## API reference

All endpoints require a JWT Bearer token. The included `local-jwt-public.pem` is used to validate tokens in local and Docker Compose environments.

```
Authorization: Bearer <token>
```

Two roles are required:

| Role | Access |
|---|---|
| `FraudEngineRead` | GET endpoints |
| `FraudEngineWrite` | GET + PATCH endpoints |

### Fraud flags — `/api/v1/fraud-flags`

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Read | Paginated list of fraud flags (`?page=`, `?size=`, `?sort=`) |
| GET | `/{flagId}` | Read | Single flag with full transaction detail |

### Rules — `/api/v1/rules`

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/` | Read | List all rules with current configuration |
| PATCH | `/{ruleId}` | Write | Update rule config — invalidates the engine cache immediately |

PATCH body (all fields optional):

```json
{
  "enabled": true,
  "severity": "HIGH",
  "threshold": 50000.00,
  "windowMinutes": 60
}
```

### Transactions — `/api/v1/transactions`

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/{transactionId}` | Read | Single transaction |
| GET | `/{transactionId}/flags` | Read | All fraud flags for a transaction |

### Health & observability

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Liveness + DB connectivity |
| `GET /actuator/metrics` | Micrometer metrics |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |
| `GET /api-docs` | OpenAPI JSON spec |

---

## Architecture

```
Kafka topic: {environment}-transactions-categorized
        │
        ▼
TransactionEventsConsumer          @KafkaListener + @KafkaHandler dispatch
        │
        ▼
TransactionIngestionService        Idempotency check (UNIQUE constraint on idempotency_key)
        │
        ▼
RuleEvaluationPipeline             Full evaluation — all active rules run in parallel
        │                          Java 21 virtual threads (newVirtualThreadPerTaskExecutor)
        ├── VelocityRule
        ├── LargeAmountRule
        ├── UnusualHourRule
        ├── DuplicateTransactionRule
        ├── GeographicAnomalyRule
        └── CategoryMismatchRule
                │
                ▼
        RuleRegistry               DB-backed config cache, volatile reference swap
                │
                ▼
        FraudFlagService           Persists FLAGGED results to PostgreSQL
                │
                ▼
        REST API                   JWT-secured, role-based (FraudEngineRead / FraudEngineWrite)
```

### Key design decisions

**Full pipeline evaluation (no short-circuit)**
Every rule runs even if an earlier one already flagged the transaction. A fraud analyst needs the complete signal picture — `GEOGRAPHIC_ANOMALY` + `VELOCITY` firing together carries different weight than either alone.

**Rule-as-data with hot reload**
Rule configurations (thresholds, windows, enabled flag) live in the database. Updating a rule via `PATCH /api/v1/rules/{id}` invalidates the in-memory `RuleRegistry` cache immediately — no redeployment required. Adding a new rule requires one implementation class and one Flyway migration; the pipeline, factory, and registry pick it up automatically.

**At-least-once Kafka delivery + idempotent consumer**
A UNIQUE constraint on `idempotency_key` makes the consumer safe to replay. If a message is redelivered (consumer restart, rebalance), the duplicate insert fails silently and processing continues. This avoids the overhead of the Kafka transactional producer API while providing equivalent safety for a consume-and-write-to-DB workload.

**Virtual threads for rule evaluation**
Each rule performs database I/O. `Executors.newVirtualThreadPerTaskExecutor()` allows all rules to run concurrently without exhausting a bounded platform thread pool — threads park on I/O without blocking their carrier thread.

---

## CI/CD

GitHub Actions runs on every push and pull request to `main` and `develop`:

1. Compile and run all 45 tests against a PostgreSQL service container
2. Build the Docker image

See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).
