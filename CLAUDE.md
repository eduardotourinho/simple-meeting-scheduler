# CLAUDE.md

This is a project that originally was create for a tech-interview code-challenge.

## Architecture

Spring Boot 4 / Java 21 REST API with a 3-layer architecture:

**API Layer** (`api/`) → **Service Layer** (`service/`) → **Domain Layer** (`domain/`)

- **API layer**: REST controllers + DTOs + global exception handler. Controllers delegate immediately to services.
- **Service layer**: Use-case orchestration and DTO mapping. Contains custom exceptions (e.g. `DuplicateEmailException`, `TimeSlotNotAvailableException`).
- **Domain layer**: JPA entities (`User`, `TimeSlot`, `Meeting`, `MeetingParticipant`) and Spring Data repositories. Schema managed by Flyway migrations in `src/main/resources/db/migration/`.
- **Config layer** (`config/`): Security (stateless), Redis cache (30-min TTL), OpenTelemetry tracing, `AuthorizationTokenFilter` (extracts user UUID from `Authorization: Bearer <user-id>` header), `TraceIdFilter`.

## Key Design Decisions

- **Authorization**: Uses a plain user UUID as the bearer token — intentionally simple (not JWT). The `Authorization: Bearer <userId>` header is how admin endpoints identify the caller.
- **Caching**: Redis with Jackson serialization. Cache config is in `CacheConfig.java`.
- **Observability**: OpenTelemetry tracing (10% sampling), Prometheus metrics, Grafana LGTM stack available via Docker Compose on port 3000.
- **Testing**: Integration tests use Testcontainers (PostgreSQL, Redis spun up per test). Tests live alongside the layer they test (`api/`, `service/`, `performance/`).
- **Known gaps** (from README): DTO mapping is done in services instead of a dedicated mapper layer; no meeting cancellation; no admin endpoint to list meetings.

