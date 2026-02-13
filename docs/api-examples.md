# API request examples

Base URL (when running with Docker):

- `http://localhost:8080`

All examples below use `curl`.

## Users

### Create a user

Request:

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "timezone": "Europe/Berlin"
  }'
```

Response (example):

```json
{
  "id": "<user-uuid>",
  "name": "Alice",
  "email": "alice@example.com",
  "timezone": "Europe/Berlin"
}
```

## Admin time slot management (requires Authorization)

Admin endpoints require an **Authorization header**.

Expected format:

- `Authorization: Bearer <user-uuid>`

> Obs: For simplicity, the `<user-uuid>` should be the ID of the user that created the time slot. In a real application, this should be changed to handle a JWT of the authenticated user.  

### Create multiple time slots

Request:

```bash
curl -X POST "http://localhost:8080/api/admin/time-slots" \
  -H "Authorization: Bearer <user-uuid>" \
  -H "Content-Type: application/json" \
  -d '{
    "slots": [
      {
        "startTime": "2026-02-13T10:00:00Z",
        "endTime": "2026-02-13T11:00:00Z",
        "status": "AVAILABLE"
      },
      {
        "startTime": "2026-02-13T12:00:00Z",
        "endTime": "2026-02-13T13:00:00Z",
        "status": "AVAILABLE"
      }
    ]
  }'
```

### Get a specific time slot

```bash
curl "http://localhost:8080/api/admin/time-slots/<time-slot-uuid>" \
  -H "Authorization: Bearer <user-uuid>"
```

### Update a time slot

```bash
curl -X PUT "http://localhost:8080/api/admin/time-slots/<time-slot-uuid>" \
  -H "Authorization: Bearer <user-uuid>" \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-02-13T10:30:00Z",
    "endTime": "2026-02-13T11:30:00Z",
    "status": "BUSY"
  }'
```

### Delete a time slot

```bash
curl -X DELETE "http://localhost:8080/api/admin/time-slots/<time-slot-uuid>" \
  -H "Authorization: Bearer <user-uuid>"
```

## Public time slots

### List a user’s time slots (paginated)

Request:

```bash
curl "http://localhost:8080/api/time-slots/user/<user-uuid>?page=0&size=10"
```

With optional filters:

- `startDate` (ISO date, e.g. `2026-02-13`)
- `endDate` (ISO date, e.g. `2026-02-20`)
- `status` (`AVAILABLE`, `BUSY`, `BOOKED`, ... depending on the enum values)

```bash
curl "http://localhost:8080/api/time-slots/user/<user-uuid>?startDate=2026-02-13&endDate=2026-02-20&status=AVAILABLE&page=0&size=10"
```

### Create a meeting by booking a time slot

Request:

```bash
curl -X POST "http://localhost:8080/api/time-slots/<time-slot-uuid>/meetings" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Intro call",
    "description": "Optional description",
    "participants": [
      { "name": "Bob", "email": "bob@example.com" },
      { "name": "Charlie", "email": "charlie@example.com" }
    ]
  }'
```

Response (example):

```json
{
  "meetingId": "<meeting-uuid>",
  "timeSlotId": "<time-slot-uuid>",
  "title": "Intro call",
  "description": "Optional description",
  "organizerId": "<organizer-user-uuid>",
  "organizerEmail": "alice@example.com",
  "startTime": "2026-02-13T10:00:00Z",
  "endTime": "2026-02-13T11:00:00Z",
  "participants": [
    {
      "participantId": "<participant-uuid>",
      "name": "Bob",
      "email": "bob@example.com",
      "type": "EXTERNAL",
      "status": "INVITED"
    }
  ],
  "createdAt": "2026-02-13T12:00:00"
}
```