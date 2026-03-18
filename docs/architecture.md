# Architecture

## Overview

Simple Meeting Scheduler is a Spring Boot 4 / Java 21 REST API built around a 3-layer architecture.

```
HTTP Request
     │
     ▼
┌─────────────┐
│  API Layer  │  Controllers + DTOs + GlobalExceptionHandler
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Service Layer  │  Use-case orchestration, DTO mapping, custom exceptions
└──────┬──────────┘
       │
       ▼
┌──────────────┐
│ Domain Layer │  JPA entities + Spring Data repositories
└──────────────┘
       │
       ▼
  PostgreSQL
```

## Layers

### API Layer (`api/`)

Controllers receive HTTP requests, delegate immediately to services, and return responses. No business logic lives here.

| Controller | Responsibility |
|---|---|
| `UserController` | Create users |
| `TimeSlotController` | Public time slot queries and meeting booking |
| `TimeSlotAdminController` | CRUD for the authenticated user's own time slots |

DTOs in `api/dto/` are the only types exchanged across the API boundary — JPA entities are never returned directly.

A `GlobalExceptionHandler` translates service exceptions into structured `ErrorResponse` JSON.

### Service Layer (`service/`)

| Service | Responsibility |
|---|---|
| `CalendarService` | Public time slot queries (with caching) and meeting creation |
| `TimeSlotAdminApiService` | Admin CRUD for time slots (with cache eviction) |
| `MeetingService` | Persist meetings and build participants |
| `TimeSlotService` | Low-level time slot persistence and overlap detection |
| `UserApiService` | User creation |
| `UserService` | User lookups |
| `AuthService` | Resolves the authenticated user UUID from the security context |

Custom exceptions (e.g. `DuplicateEmailException`, `TimeSlotNotAvailableException`, `TimeSlotOverlapException`, `TimeSlotNotFoundException`, `UserNotFoundException`) are thrown here and caught by the global exception handler.

### Domain Layer (`domain/`)

#### Entities

| Entity | Table | Notes |
|---|---|---|
| `User` | `users` | UUID PK, unique email, IANA timezone string |
| `TimeSlot` | `time_slots` | Belongs to a `User`; status: `AVAILABLE`, `BUSY`, `BOOKED`; `@Version` on `updated_at` for optimistic locking |
| `Meeting` | `meetings` | 1-to-1 with `TimeSlot`; has an organizer (`User`) and a list of `MeetingParticipant` |
| `MeetingParticipant` | `meeting_participants` | Either `INTERNAL` (linked to a `User`) or `EXTERNAL` (name + email only); status: `INVITED`, `ACCEPTED`, `DECLINED` |

#### Repositories

Spring Data JPA repositories for each entity live in `domain/repository/`.

#### Schema management

Flyway runs automatically on startup. Migrations are in `src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| `V1` | `users` table |
| `V2` | `time_slots` table (with overlap-prevention unique index and status check constraint) |
| `V3` | `meetings` table |
| `V4` | `meeting_participants` table (with check constraint enforcing internal vs. external data rules) |
| `V5` | `updated_at` triggers |

## Config Layer (`config/`)

### Security

`SecurityConfig` sets up a stateless, session-less filter chain:

- `GET /api/time-slots/**`, `POST /api/time-slots/**`, `POST /api/users`, `/scalar/**`, `/api-docs/**`, `/actuator/**` — open to all.
- `POST /api/admin/**`, `PUT /api/admin/**`, `DELETE /api/admin/**` — require authentication.

`AuthorizationTokenFilter` (runs before `UsernamePasswordAuthenticationFilter`) intercepts admin requests and extracts the user UUID from the `Authorization: Bearer <user-uuid>` header. An invalid or missing token returns `401` immediately. This is intentionally simple — not JWT.

### Caching

`CacheConfig` configures a `RedisCacheManager` with:

- **TTL**: 30 minutes for all entries.
- **Serialization**: `StringRedisSerializer` for keys, `GenericJacksonJsonRedisSerializer` (with polymorphic type info scoped to `dev.eduardo.scheduler` and `java.util`) for values.
- **Transaction-aware**: cache operations participate in the surrounding transaction.

Cache names in use:

| Cache name | Populated by | Evicted by |
|---|---|---|
| `userTimeSlotsPageable` | `CalendarService.getUserTimeSlotsPageable` | `CalendarService.createMeeting`, `TimeSlotAdminApiService` write operations |
| `adminTimeSlots` | `TimeSlotAdminApiService.getTimeSlot` | `TimeSlotAdminApiService` write operations |

### Observability

| Signal | Tool |
|---|---|
| Tracing | OpenTelemetry (10% sampling); `TraceIdFilter` injects trace IDs into HTTP responses |
| Metrics | Micrometer with JVM metrics (CPU, memory, threads, class loading) exported via Prometheus |
| Logging | Logback with OTel log appender (`InstallOpenTelemetryAppender`) |
| Dashboard | Grafana LGTM stack on port 3000 (available via Docker Compose) |

## Request flow — booking a meeting

```
Client
  │  POST /api/time-slots/{timeSlotId}/meetings
  ▼
TimeSlotController
  │  delegates to
  ▼
CalendarService.createMeeting()
  ├── TimeSlotService.findById()          — load & validate slot is AVAILABLE
  ├── MeetingService.saveMeeting()        — persist Meeting
  ├── MeetingService.create*Participant() — build participants (INTERNAL if email matches a User, else EXTERNAL)
  ├── TimeSlotService.updateSlot()        — mark slot as BOOKED
  └── @CacheEvict userTimeSlotsPageable   — invalidate public calendar cache
  │
  ▼
CreateMeetingResponse (JSON)
```

## Spring profiles

| Profile | Purpose |
|---|---|
| *(default)* | Local development — expects an external PostgreSQL and Redis |
| `dev` | `application-dev.yaml` overrides |
| `docker` | `application-docker.yaml` — reads connection details from Docker Compose environment variables |

## Testing strategy

Integration tests use Testcontainers to spin up real PostgreSQL and Redis instances per test run. Tests are colocated with the layer they cover:

| Path | Scope |
|---|---|
| `api/` tests | Full HTTP integration (MockMvc + Testcontainers) |
| `service/` tests | Service unit tests and service integration tests |
| `performance/` | Cache performance / regression tests |

## Known limitations

- `Authorization` accepts a plain UUID, not a JWT — a production system would use signed JWTs or an external identity provider.
- DTO mapping is done inside services rather than a dedicated mapper layer.
- No endpoint for an organizer to list their scheduled meetings.
- No meeting cancellation support.