# Simple Meeting Scheduler

## About the project

This is a simple meeting scheduler API that allows users to create and manage their time slots, and book meetings with other users.

### Tech stack

- Spring Boot (4.0.2) 
- Java 21
- PostgreSQL
- Redis (caching), 
- Grafana for observability (Metrics, Tracing and Logging).

### Project structure

See [docs/project-structure.md](docs/project-structure.md) for details.

## Running with Docker

This repo includes:

- `docker-compose.yaml` (Postgres + Redis + Grafana OTEL LGTM + the service)
- `Dockerfile` (builds and runs the Spring Boot app via Gradle)

### 1) Start the stack

From the repository root:

```bash
docker compose up -d service
```

It will start the service and the dependencies (Postgres, Redis and Grafana) automatically:

- **API**: `http://localhost:8080`
- **Grafana** (Monitoring and Observability): `http://localhost:3000`
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`

### 2) Stopping

```bash
docker compose down
```


### Usage

1. Create a new user via the `Users` API
2. Create time slots for the user using the `Time Slot Admin` APIs. You will need to pass an existing `user's id` in the Authorization header.
3. Fetch the user's time slots via `Time Slots` APIs
4. Book a time slot for the user via `Time Slots` APIs

### API docs

After the service is started, you can access the OpenAPI docs and client accessing`http://localhost:8080/scalar`.

For more details on the API, see [docs/api-examples.md](docs/api-examples.md).


## Database migrations

Flyway runs automatically on startup. Migrations live in:

`src/main/resources/db/migration`

## Tests

Run all tests:

```bash
./gradlew test
```

## Improvements

This is non-comprehensive list of possible improvements and missing features.

- [feature] Add API to get the user's scheduled meetings from the Admin endpoints, with information about the participants.
- [feature] Ability to cancel a meeting before the scheduled time.
- [security] The Authorization header should be a JWT token of the authenticated user.
- [code quality] The project structure is not optimal. It should be refactored to better separate concerns and domains.
- [code quality] All the mappings are done inside the Services. It should be refactored to better separate concerns and domains.
- [observability] Improve traceability adding spans related to the business logic. 
