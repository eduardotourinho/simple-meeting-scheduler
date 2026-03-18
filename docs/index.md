# Meeting Scheduler

A REST API for managing time slots and booking meetings between users, built with Spring Boot and backed by PostgreSQL, Redis caching, and full Grafana observability.

## Tech stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 4.0.2 |
| Language | Java 21 |
| Database | PostgreSQL |
| Cache | Redis |
| Observability | Grafana LGTM (Metrics, Tracing, Logging) |
| Runtime | Java 21 |

## Running with Docker

```bash
# Start the full stack (API + Postgres + Redis + Grafana)
docker compose --profile infrastructure up -d service
```

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| API docs (Scalar) | http://localhost:8080/scalar |
| Grafana | http://localhost:3000 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

## Core workflow

1. Create a user via `POST /api/users`
2. Create time slots for that user via `POST /api/admin/time-slots` (requires `Authorization: Bearer <user-id>`)
3. Browse available slots via `GET /api/time-slots/user/{userId}`
4. Book a meeting via `POST /api/time-slots/{timeSlotId}/meetings`

## Database migrations

Flyway runs automatically on startup. Migrations are in `src/main/resources/db/migration`.

## Tests

```bash
./gradlew test
```

Integration tests use Testcontainers.

## Known limitations & future improvements

- `Authorization` header accepts a plain user UUID — should be replaced with JWT authentication
- Mappings are done inside services — should be refactored to a dedicated mapper layer
- No endpoint to list a user's scheduled meetings from the admin side
- No meeting cancellation support
