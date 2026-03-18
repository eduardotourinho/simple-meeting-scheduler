# Meeting Scheduler

A portfolio project demonstrating REST API design with Spring Boot 4, Redis caching, OpenTelemetry observability, and Testcontainers-based integration testing. The service allows users to manage availability time slots and book meetings with other users.

## Tech Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.2 |
| PostgreSQL | 17 |
| Redis | latest |
| Flyway | (managed by Spring Boot) |
| Testcontainers | (managed by Spring Boot) |
| OpenTelemetry | (via OTEL Java agent) |
| Grafana LGTM | latest |

## Architecture

The service follows a 3-layer architecture:

```
API Layer (api/)  →  Service Layer (service/)  →  Domain Layer (domain/)
```

- **API layer** — REST controllers, DTOs, and a global exception handler. Controllers delegate immediately to services without containing business logic.
- **Service layer** — use-case orchestration, DTO mapping, and custom exceptions (`DuplicateEmailException`, `TimeSlotNotAvailableException`).
- **Domain layer** — JPA entities (`User`, `TimeSlot`, `Meeting`, `MeetingParticipant`) and Spring Data repositories. Schema is managed by Flyway migrations in `src/main/resources/db/migration/`.
- **Config layer** — stateless security, Redis cache configuration, OpenTelemetry tracing, `AuthorizationTokenFilter`, and `TraceIdFilter`.

### Key Design Decisions

- **Authorization** — uses a plain user UUID as the bearer token (`Authorization: Bearer <userId>`). Intentionally simple: it identifies the caller without introducing JWT infrastructure.
- **Caching** — Redis with Jackson serialization and a 30-minute TTL, configured in `CacheConfig.java`.
- **Observability** — OpenTelemetry tracing at 10% sampling rate, Prometheus metrics, and a full Grafana LGTM stack available via Docker Compose.
- **Testing** — integration tests use Testcontainers to spin up real PostgreSQL and Redis instances per test class. Tests live alongside the layer they cover (`api/`, `service/`, `performance/`).

See [docs/architecture.md](docs/architecture.md) for a deeper breakdown.

## Running

Start the full stack (PostgreSQL, Redis, Grafana LGTM, Spring Boot service) with:

```bash
docker compose --profile infrastructure up -d service
```

| Endpoint | URL |
|---|---|
| API | http://localhost:8080 |
| API docs (Scalar) | http://localhost:8080/scalar |
| Grafana | http://localhost:3000 |

Stop everything:

```bash
docker compose down
```

Run tests:

```bash
./gradlew test
```

## Known Limitations

- **UUID bearer token, not JWT** — the `Authorization` header carries a raw user UUID. A real system would use signed JWTs or an external identity provider.
- **DTO mapping in services** — mapping between DTOs and entities is done inside the service layer rather than in a dedicated mapper layer (e.g. MapStruct).
- **No meeting cancellation** — once a time slot is booked, there is no endpoint to cancel the resulting meeting.
- **No admin list-meetings endpoint** — there is no way to list all meetings for a given user from the admin API; only individual time slot lookups are supported.