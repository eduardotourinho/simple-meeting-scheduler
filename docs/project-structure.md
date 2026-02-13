# Project Structure

The source tree follows a layered architecture:

- **`src/main/java/dev/eduardo/scheduler/api`**
    - Spring MVC controllers (HTTP layer).
    - Request/response mapping and HTTP concerns only.

- **`src/main/java/dev/eduardo/scheduler/api/dto`**
    - DTOs used by controllers and API-facing services.
    - Prefer using DTOs at boundaries (controllers/services) instead of returning JPA entities.

- **`src/main/java/dev/eduardo/scheduler/service`**
    - Application services (use-case orchestration).
    - Typical responsibilities:
        - Validate high-level flow.
        - Call domain services and repositories.
        - Map entities to DTOs.
    - Examples:
        - `CalendarService` (public calendar/time-slot queries + meeting creation)
        - `TimeSlotAdminApiService` (admin operations)

- **`src/main/java/dev/eduardo/scheduler/domain`**
    - Domain model and persistence:
        - `domain/entities`: JPA entities.
        - `domain/repository`: Spring Data repositories.

- **`src/main/java/dev/eduardo/scheduler/config`**
    - Spring configuration (security, beans, etc.).

- **`src/main/resources`**
    - `application.yaml`: base configuration.
    - `db/migration`: Flyway migrations.

- **`src/test/java`**
    - Unit/integration tests.
    - Testcontainers is used for integration testing in several tests.