# Institute Management Service

A Spring Boot microservice for managing an institute directory. It exposes a versioned REST API, stores institute records in PostgreSQL, and reports health through Spring Boot Actuator.

## Requirements

- Java 17+
- Maven 3.9+
- Docker with Docker Compose (for the containerized setup)

## Run with Docker Compose

```powershell
$env:DB_PASSWORD = "choose-a-local-password"
docker compose up --build
```

The management dashboard is available at `http://localhost:8080/`. The API is available at `http://localhost:8080/api/v1/institutes`; health is available at `http://localhost:8080/actuator/health`.

## Run locally

Start a PostgreSQL database, then set the connection variables and run the service:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "institutes"
$env:DB_USER = "institute"
$env:DB_PASSWORD = "your-database-password"
mvn spring-boot:run
```

Flyway applies database migrations at startup. Hibernate validates the schema and does not modify it automatically.

## API

All endpoints use `/api/v1/institutes`.

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/institutes` | Create an institute (`201 Created`, with a `Location` header) |
| `GET` | `/api/v1/institutes` | List institutes ordered by name |
| `GET` | `/api/v1/institutes/{id}` | Get an institute by UUID |
| `PUT` | `/api/v1/institutes/{id}` | Replace an institute |
| `DELETE` | `/api/v1/institutes/{id}` | Delete an institute (`204 No Content`) |

Create and update requests accept `name` and `code` (required), plus optional `email`, `phone`, `website`, and `address` fields. Codes are trimmed, stored in uppercase, and unique without regard to input case. Invalid requests return `400`, duplicate codes return `409`, and unknown IDs return `404` using Problem Details responses.

Example:

```json
{
  "name": "North Campus",
  "code": "north-01",
  "email": "office@example.org",
  "phone": "+1-555-0100",
  "website": "https://example.org",
  "address": "1 University Way"
}
```

## Tests

```powershell
mvn test
```

Tests use an in-memory H2 database and do not require PostgreSQL.
