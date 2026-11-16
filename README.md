# Institute Management Service

A Spring Boot microservice for managing an institute directory. It exposes a versioned REST API, stores institute records in PostgreSQL, and reports health through Spring Boot Actuator.

## Requirements

- Java 25+
- Maven 3.9+
- Docker with Docker Compose (for the containerized setup)

## Build the Docker image

From the repository root, build the service image with:

```powershell
docker build -t institute-management-service:latest .
```

## Run with Docker Compose

The [compose.yaml](./compose.yaml) file starts the Spring Boot institute service and PostgreSQL. It builds the service image, waits for PostgreSQL to become healthy, and keeps database data in a named Docker volume.

Set a database password, then build and start both containers from the repository root:

```powershell
$env:DB_PASSWORD = "choose-a-local-password"
docker compose up --build --detach
```

Check container status and follow the service logs:

```powershell
docker compose ps
docker compose logs -f institute-service
```

Stop the containers when finished:

```powershell
docker compose down
```

The management dashboard is available at `http://localhost:8080/`. The API is available at `http://localhost:8080/api/v1/institutes`; health is available at `http://localhost:8080/actuator/health`. PostgreSQL is exposed on port `5432`; its data remains in the `institute-data` volume after stopping the containers.

## Deploy to Kubernetes

The [k8s.yaml](./k8s.yaml) manifest deploys the institute service and PostgreSQL, including a persistent volume claim for database data. The cluster must have a default StorageClass for the claim to be provisioned.

Build the service image and make sure it is available to your Kubernetes cluster. For a local cluster, use the image-loading workflow supported by that cluster; for a remote cluster, push the image to a registry and update the image in `k8s.yaml`.

Create the database password Secret before applying the manifest:

```powershell
kubectl create secret generic institute-db-secret --from-literal=DB_PASSWORD="choose-a-strong-password"
kubectl apply -f k8s.yaml
```

Check deployment status and open the dashboard locally:

```powershell
kubectl get deployments,pods,services
kubectl port-forward service/institute-service 8080:8080
```

The dashboard is available at `http://localhost:8080/`. The Secret is managed separately so a database password does not need to be stored in the manifest.

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

## Academic management

The dashboard includes configurable course levels for each institute, courses, course sections, and students. Open **Classes & scores** to enroll students in a section, add named assessments, and enter or update a student's score. Scores must be between zero and the assessment's maximum, and only students enrolled in that class can receive a score. The class roster calculates each student's graded points, possible points, and percentage.

Academic endpoints:

| Method | Path | Purpose |
|---|---|---|
| `GET`, `POST` | `/api/v1/course-levels` | List or add institute-specific course levels |
| `GET`, `POST` | `/api/v1/courses` | List or add courses with a course level |
| `GET`, `POST` | `/api/v1/classes` | List or add course sections for an academic year |
| `GET`, `POST` | `/api/v1/students` | List or add institute students |
| `GET`, `POST` | `/api/v1/classes/{classId}/students` | View or enroll students in a class |
| `GET`, `POST` | `/api/v1/classes/{classId}/assessments` | View or add class assessments |
| `GET` | `/api/v1/classes/{classId}/scores` | View scores for the class roster |
| `PUT` | `/api/v1/assessments/{assessmentId}/scores` | Add or update a student's assessment score |

Course levels, courses, students, and enrollments are scoped to their institute. Student numbers and level codes are unique within an institute.

## Library management

The library catalog is institute-specific and shares the academic student directory. Add books with their total physical copy count, then check them out to students with a due date. Available-copy counts are calculated from active loans, and a loan cannot be created if no copy is available. Return a book from the **Library loans** view.

| Method | Path | Purpose |
|---|---|---|
| `GET`, `POST` | `/api/v1/library/books` | List available book inventory or add a title |
| `GET`, `POST` | `/api/v1/library/loans` | View circulation history or check out a book |
| `POST` | `/api/v1/library/loans/{loanId}/return` | Record a book return |

## Tests

```powershell
mvn test
```

Tests use an in-memory H2 database and do not require PostgreSQL.
