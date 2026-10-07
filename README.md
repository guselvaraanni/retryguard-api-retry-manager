# RetryGuard — Smart API Retry & Failure Tracker

RetryGuard is a Spring Boot backend service that tracks backend operations and handles
**temporary (transient) failures** using a configurable number of attempts and
**exponential backoff**. Every attempt is recorded, so you can see what failed, why,
how long it took, and whether the operation eventually recovered.

> Status: **Stage 6 — retry engine with exponential backoff (no execute endpoint yet).** Runs on `http://localhost:8082`
> (`.\mvnw.cmd spring-boot:run`). Copy `.env.example` to `.env` and set your local
> PostgreSQL credentials first. Import `postman/RetryGuard.postman_collection.json` into Postman.

---

## 1. Problem Statement

Backend services constantly call things they do not control: payment gateways, email
providers, other internal services, databases. Those calls sometimes fail for reasons
that fix themselves a moment later:

- a network timeout or dropped connection
- `503 Service Unavailable` while the other service restarts
- `429 Too Many Requests` (rate limiting)
- a database connection pool that is briefly exhausted

If the service gives up on the first failure, users see errors that did not need to
happen. If the service retries immediately and endlessly, it can overload the
struggling dependency and make the outage worse.

RetryGuard demonstrates the middle ground: **retry a limited number of times, wait
longer between each attempt, record everything, and fail cleanly when retries are
exhausted.**

### Transient vs permanent failures

| Transient (worth retrying) | Permanent (do not retry) |
|----------------------------|--------------------------|
| Timeout, connection reset  | `400 Bad Request`        |
| `503`, `429`               | `401` / `403`            |
| Temporary lock / pool full | `404 Not Found`          |

Retrying a permanent failure only wastes time — the result will never change.

---

## 2. Features (planned)

- Create, read, update, and delete retry operations
- Configure maximum attempts and initial retry delay per operation
- Execute an operation through a plain-Java retry engine
- Exponential backoff between attempts, with a safety cap on delay
- Deterministic failure simulation (`failuresBeforeSuccess`) for repeatable demos
- Per-attempt history (attempt number, status, error, timestamps)
- Statistics: success rate, recovery count, average/maximum attempts
- Consistent JSON error responses via global exception handling
- Protection against two concurrent executions of the same operation

No external APIs are called. Failures are **simulated deterministically** so every
Postman demo produces the same result.

---

## 3. Technology Stack

| Area        | Choice                                   |
|-------------|------------------------------------------|
| Language    | Java 21, SQL                             |
| Framework   | Spring Boot 3.x (Spring MVC, Spring Data JPA, Bean Validation) |
| ORM         | Hibernate (via Spring Data JPA)          |
| Database    | PostgreSQL                               |
| Build       | Maven (via Maven Wrapper `mvnw`)         |
| Tools       | Git, Postman                             |

Intentionally **not** used: Resilience4j, Spring Retry, Spring Cloud, Kafka, RabbitMQ,
Redis, Docker, Spring Security, Lombok. The retry logic is written by hand so that it
can be fully explained.

---

## 4. Architecture

Classic layered architecture. Each layer only talks to the layer directly below it.

```
            Postman (HTTP client)
                    │  JSON over HTTP
                    ▼
┌──────────────────────────────────────────┐
│ Controller layer  (@RestController)      │  HTTP mapping, validation trigger,
│                                          │  DTO in / DTO out. No business logic.
└──────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────┐
│ Service layer     (@Service)             │  Business rules, retry engine,
│                                          │  backoff, state transitions,
│                                          │  statistics, transactions.
└──────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────┐
│ Repository layer  (JpaRepository)        │  Database access only.
└──────────────────────────────────────────┘
                    │  SQL (Hibernate)
                    ▼
               PostgreSQL
```

Cross-cutting: `exception` (custom exceptions + `@RestControllerAdvice`),
`config` (retry settings), `util` (small helpers such as backoff calculation).

### Package structure

```
com.retryguard
├── controller   REST endpoints
├── service      business logic, retry engine, statistics
├── repository   Spring Data JPA interfaces
├── entity       JPA entities and enums
├── dto          request / response objects
├── exception    custom exceptions, global handler, error response
├── config       configuration properties
└── util         small stateless helpers
```

### Design rules

- Constructor injection only (no field injection, no Lombok)
- Thin controllers; business logic in services; persistence in repositories
- Entities never returned directly from controllers — DTOs only
- No infinite retries; no swallowed exceptions; no stack traces in API responses

---

## 5. Domain Model

### `RetryOperation`

One unit of work that may need retrying.

| Field                   | Type            | Meaning |
|-------------------------|-----------------|---------|
| `id`                    | `Long`          | Primary key |
| `operationName`         | `String`        | Human-readable name, e.g. `"Charge payment #42"` |
| `operationType`         | enum            | Category, e.g. `PAYMENT`, `EMAIL`, `HTTP_CALL` (used for grouping statistics) |
| `maxRetries`            | `int`           | **Maximum total attempts, including the first** (1–10) |
| `initialDelayMs`        | `long`          | Delay before the 2nd attempt; doubles afterwards |
| `failuresBeforeSuccess` | `int`           | Simulation control: how many attempts fail before one succeeds |
| `status`                | enum            | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED` |
| `totalAttempts`         | `int`           | Attempts actually made |
| `lastError`             | `String`        | Message from the most recent failed attempt |
| `recovered`             | `boolean`       | `true` if it succeeded **after** at least one failure |
| `startedAt`             | `LocalDateTime` | When execution began |
| `completedAt`           | `LocalDateTime` | When execution finished |
| `createdAt`             | `LocalDateTime` | When the record was created |

> **Naming note:** `maxRetries` is interpreted as *total attempts allowed*, so
> `maxRetries = 3` means at most 3 attempts. This matches the examples below and is
> documented explicitly to avoid the common "is the first call a retry?" ambiguity.

### `RetryAttempt` (planned for Stage 7)

| Field            | Type            |
|------------------|-----------------|
| `id`             | `Long`          |
| `retryOperation` | `RetryOperation` (many-to-one) |
| `attemptNumber`  | `int`           |
| `status`         | enum: `SUCCESS`, `FAILED` |
| `errorMessage`   | `String`        |
| `startedAt`      | `LocalDateTime` |
| `completedAt`    | `LocalDateTime` |

**Why a separate entity is worth it:** `RetryOperation` only stores the *final*
outcome. Without attempt history you cannot show *which* attempt failed, *what* the
error was each time, or *how long* the backoff waits really were. It is the
natural place to demonstrate `@OneToMany` / `@ManyToOne`, foreign keys, `mappedBy`,
lazy loading, and cascade decisions — and it stays small (one extra table, one
extra endpoint). Storing attempts as a JSON blob or a comma-separated string would
be harder to query and would teach less.

### Status lifecycle

```
 PENDING ──execute──► RUNNING ──attempt succeeds──────────► SUCCESS
                         │
                         └──all attempts fail────────────► FAILED
```

- Only `PENDING` operations can be executed (`SUCCESS` and `FAILED` are final).
- Executing an operation that is already `RUNNING` is rejected (`409 Conflict`).
- Updates (`PUT`) are only allowed while the operation is `PENDING`.

---

## 6. Database Design

```
retry_operations                         retry_attempts
────────────────────────────            ─────────────────────────────
id                  BIGSERIAL PK  ◄──┐  id                BIGSERIAL PK
operation_name      VARCHAR(100)     └──operation_id      BIGINT FK NOT NULL
operation_type      VARCHAR(30)         attempt_number    INT
max_retries         INT                 status            VARCHAR(20)
initial_delay_ms    BIGINT              error_message     VARCHAR(500)
failures_before_success INT             started_at        TIMESTAMP
status              VARCHAR(20)         completed_at      TIMESTAMP
total_attempts      INT
last_error          VARCHAR(500)        UNIQUE (operation_id, attempt_number)
recovered           BOOLEAN
started_at          TIMESTAMP
completed_at        TIMESTAMP
created_at          TIMESTAMP
```

- Enums are stored as **strings** (`@Enumerated(EnumType.STRING)`), not ordinals, so
  reordering enum constants can never silently corrupt existing rows.
- Relationship: `retry_operations 1 ──── * retry_attempts`.

---

## 7. Retry Algorithm

### Exponential backoff

After a failed attempt, wait before trying again. The wait doubles each time:

```
delay(n) = initialDelayMs × 2^(n − 1)      n = number of the attempt that just failed
delay    = min(delay, maxDelayMs)          safety cap (configurable)
```

With `initialDelayMs = 100`:

| Attempt | Result  | Wait before next attempt |
|---------|---------|--------------------------|
| 1       | FAILED  | 100 ms                   |
| 2       | FAILED  | 200 ms                   |
| 3       | FAILED  | 400 ms                   |
| 4       | ...     | 800 ms                   |

There is **no wait after the final attempt** — once retries are exhausted the
operation is marked `FAILED` immediately.

### Why not retry immediately?

If 1,000 clients all retry instantly against a struggling service, it receives a
burst of traffic exactly when it is least able to handle it (a "retry storm").
Backoff spreads retries out and gives the dependency time to recover.

### Why must retries be limited?

Some failures never heal. Without a limit, a request thread would be stuck forever,
resources leak, and the caller never receives an answer. A bounded retry count
guarantees the operation always ends in a final state (`SUCCESS` or `FAILED`).

### Pseudocode

```
mark operation RUNNING, set startedAt
for attempt = 1 .. maxRetries:
    record attempt start
    if simulatedCall(attempt) succeeds:
        record attempt SUCCESS
        mark operation SUCCESS, recovered = (attempt > 1)
        stop
    else:
        record attempt FAILED with error message
        if attempt < maxRetries:
            sleep(min(initialDelayMs × 2^(attempt − 1), maxDelayMs))
mark operation FAILED if no attempt succeeded
set totalAttempts, lastError, completedAt
```

### Deterministic simulation

`simulatedCall(attempt)` fails while `attempt <= failuresBeforeSuccess`.

| failuresBeforeSuccess | maxRetries | Attempts                       | Final   | recovered |
|-----------------------|------------|--------------------------------|---------|-----------|
| 0                     | 3          | SUCCESS                        | SUCCESS | false     |
| 2                     | 3          | FAILED, FAILED, SUCCESS        | SUCCESS | true      |
| 5                     | 3          | FAILED, FAILED, FAILED         | FAILED  | false     |

---

## 8. Application Flow (execute an operation)

```
POST /api/operations/{id}/execute
  │
  ▼
Controller ── calls ──► RetryExecutionService.execute(id)
                           │
                           ├─ load operation (404 if missing)
                           ├─ check status is PENDING (409 otherwise)
                           ├─ atomically switch PENDING → RUNNING
                           ├─ loop attempts:
                           │     simulate call → record RetryAttempt
                           │     on failure: compute backoff, Thread.sleep
                           ├─ set final status, totalAttempts, recovered, lastError
                           └─ save and return ExecutionResultResponse
  │
  ▼
200 OK  { operationId, status, totalAttempts, recovered, lastError, durationMs }
```

The retry loop sleeps, so it should **not** hold one database transaction open for
the whole execution. Short transactions are used for the state changes instead
(discussed in Stages 8 and 11).

---

## 9. Planned REST API

| Method | Endpoint                          | Purpose                          | Success |
|--------|-----------------------------------|----------------------------------|---------|
| GET    | `/api/ping`                       | Health check                     | 200 |
| POST   | `/api/operations`                 | Create operation                 | 201 |
| GET    | `/api/operations`                 | List operations                  | 200 |
| GET    | `/api/operations/{id}`            | Get one operation                | 200 |
| PUT    | `/api/operations/{id}`            | Update a `PENDING` operation     | 200 |
| DELETE | `/api/operations/{id}`            | Delete operation                 | 204 |
| POST   | `/api/operations/{id}/execute`    | Run the retry engine             | 200 |
| GET    | `/api/operations/{id}/attempts`   | Attempt history                  | 200 |
| GET    | `/api/operations/statistics`      | Aggregated retry statistics      | 200 |

Error codes: `400` validation error, `404` operation not found,
`409` operation already running / not executable.

### Example create request

```json
POST /api/operations
{
  "operationName": "Charge payment #42",
  "operationType": "PAYMENT",
  "maxRetries": 3,
  "initialDelayMs": 100,
  "failuresBeforeSuccess": 2
}
```

### Example error response

```json
{
  "timestamp": "2026-10-07T17:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Retry operation not found with id: 99",
  "path": "/api/operations/99"
}
```

---

## 10. Java & Spring Concepts Demonstrated

| Concept | Where it appears |
|---------|------------------|
| OOP, encapsulation | Entities, services, DTOs |
| Enums | `OperationStatus`, `OperationType`, `AttemptStatus` |
| Collections & Generics | `List<RetryAttempt>`, `Map<OperationType, Long>`, `JpaRepository<RetryOperation, Long>` |
| Optional | `findById(...).orElseThrow(...)` |
| Exceptions | Custom unchecked exceptions, global handler, simulated attempt failures |
| java.time | `LocalDateTime`, `Duration` for execution timing |
| Streams & Lambdas | Statistics: `filter`, `count`, `groupingBy`, `averagingInt`, `max` |
| Method references | `RetryOperation::getTotalAttempts`, DTO mappers |
| Concurrency | `Thread.sleep` in backoff; preventing double execution of one operation (Stage 10) |
| Spring IoC / DI | Constructor-injected beans |
| Spring MVC | `@RestController`, `@RequestMapping`, `ResponseEntity` |
| Bean Validation | `@NotBlank`, `@Min`, `@Max`, `@Valid` |
| Spring Data JPA | Derived queries, custom `@Query` |
| Hibernate relationships | `@OneToMany(mappedBy = ...)`, `@ManyToOne(fetch = LAZY)` |
| Transactions | `@Transactional` only where needed |

---

## 11. Development Stages

| Stage | Goal | Commit message |
|-------|------|----------------|
| 0  | Project definition & architecture | `docs: define RetryGuard architecture` |
| 1  | Spring Boot project setup, `GET /api/ping` | `feat: initialize RetryGuard Spring Boot project` |
| 2  | PostgreSQL, `RetryOperation` entity, repository | `feat: configure PostgreSQL and retry operation entity` |
| 3  | CRUD APIs with DTOs and validation | `feat: add retry operation CRUD APIs` |
| 4  | Custom exceptions & global handler | `feat: add global exception handling` |
| 5  | Retry execution engine (no backoff yet) | `feat: implement retry execution engine` |
| 6  | Exponential backoff | `feat: add exponential retry backoff` |
| 7  | `RetryAttempt` history | `feat: add retry attempt history` |
| 8  | Execute endpoint | `feat: add retry execution API` |
| 9  | Statistics with Streams | `feat: add retry statistics with streams` |
| 10 | Concurrency review & hardening | `feat: harden retry execution for concurrent requests` |
| 11 | Cleanup & transaction review | `refactor: clean up RetryGuard service design` |
| 12 | Full Postman verification | `test: finalize RetryGuard verification` |
| 13 | Final documentation | `docs: finalize RetryGuard documentation` |
