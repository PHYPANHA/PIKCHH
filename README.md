# PIKCHH Institute Management

A Spring Boot application for managing institutes, academic courses and classes, student scores, and library circulation. It provides a browser dashboard, a versioned REST API, PostgreSQL persistence, and health endpoints.

## Features

- **Institute directory:** Create and manage institute contact details.
- **Academic management:** Define course levels per institute, create courses and sections, register students, enroll them in classes, and record assessment scores.
- **Score summaries:** View each student's graded points, possible points, and percentage for a class.
- **Library management:** Maintain institute-specific book catalogs, check books out to institute students, track due dates, and record returns. Available copies are calculated from active loans.
- **Data isolation:** Course levels, students, books, and class enrollment are associated with their institute. Students and books from different institutes cannot be combined in a class enrollment or library loan.

## Technology

- Java 25 (LTS)
- Spring Boot 3.5
- Maven 3.9+
- PostgreSQL 16
- Flyway database migrations
- Docker Compose or Kubernetes for deployment

## Run with Docker Compose

Docker Compose builds the service image and starts the service with PostgreSQL. From the repository root, set a local database password and start the application:

```powershell
$env:DB_PASSWORD = "choose-a-local-password"
docker compose up --build --detach
```

Open the dashboard at <http://localhost:8080/>. The service health endpoint is <http://localhost:8080/actuator/health>. PostgreSQL is available on port `5432`.

Check service status and logs:

```powershell
docker compose ps
docker compose logs -f institute-service
```

Stop the containers:

```powershell
docker compose down
```

The database data is stored in the `institute-data` volume and remains after `docker compose down`. To intentionally remove that data as well, use `docker compose down --volumes`.

## Run locally

Requirements: Java 25 and Maven 3.9+. Start the Compose PostgreSQL service:

```powershell
docker compose up --detach postgres
```

Set the connection settings and run Spring Boot:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "institutes"
$env:DB_USER = "institute"
$env:DB_PASSWORD = "institute-local"
mvn spring-boot:run
```

The dashboard is at <http://localhost:8080/>. Flyway applies schema migrations at startup; Hibernate validates the schema without modifying it.

To build the container image separately:

```powershell
docker build -t institute-management-service:latest .
```

## Dashboard workflow

1. Add an institute.
2. Add course levels for that institute, then create courses under those levels.
3. Add students and create class sections for courses.
4. Open **Classes & scores** and choose **Manage** to enroll students, create assessments, and save scores.
5. Add books in **Library catalog** and manage checkouts and returns in **Library loans**.

## REST API

All API routes use the `/api/v1` prefix. Requests and responses use JSON. Resource validation errors return `400`; missing resources return `404`; duplicate or unavailable resources return `409` using Problem Details responses.

### Institutes

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/institutes` | List institutes, ordered by name |
| `POST` | `/api/v1/institutes` | Create an institute |
| `GET` | `/api/v1/institutes/{id}` | Get an institute by UUID |
| `PUT` | `/api/v1/institutes/{id}` | Replace an institute |
| `DELETE` | `/api/v1/institutes/{id}` | Delete an institute |

Institute create/update requests require `name` and `code`; `email`, `phone`, `website`, and `address` are optional. Codes are trimmed, stored in uppercase, and unique without regard to case.

### Academics

| Method | Path | Purpose |
|---|---|---|
| `GET`, `POST` | `/api/v1/course-levels` | List or add institute-specific course levels |
| `GET`, `POST` | `/api/v1/courses` | List or add courses with a course level |
| `GET`, `POST` | `/api/v1/classes` | List or add course sections for an academic year |
| `GET`, `POST` | `/api/v1/students` | List or add institute students |
| `GET`, `POST` | `/api/v1/classes/{classId}/students` | View or enroll students in a class |
| `GET`, `POST` | `/api/v1/classes/{classId}/assessments` | View or add class assessments |
| `GET` | `/api/v1/classes/{classId}/scores` | View scores for enrolled students |
| `PUT` | `/api/v1/assessments/{assessmentId}/scores` | Add or update a student's assessment score |

Course-level requests contain `instituteId`, `code`, and `name`. Course requests contain `code`, `name`, and `levelId`; class requests contain `courseId`, `name`, and `academicYear`. Student requests contain `instituteId`, `studentNumber`, and `name`, with an optional `email`.

An assessment has a `name` and positive `maxScore`. Submit a score with `studentId` and `score`; scores must be between zero and the assessment maximum, and the student must be enrolled in the assessment's class.

### Library

| Method | Path | Purpose |
|---|---|---|
| `GET`, `POST` | `/api/v1/library/books` | List book inventory or add a title |
| `GET`, `POST` | `/api/v1/library/loans` | View loan history or check out a book |
| `POST` | `/api/v1/library/loans/{loanId}/return` | Record a book return |

Book requests contain `instituteId`, `isbn`, `title`, `author`, and `totalCopies`. Checkout requests contain `bookId`, `studentId`, and a future `dueDate` (`YYYY-MM-DD`). Checkout is rejected when no copies are available or the student and book belong to different institutes.

## Kubernetes

The [k8s.yaml](./k8s.yaml) manifest deploys the service and PostgreSQL. The cluster needs a default StorageClass for the database volume.

Build and make the service image available to the cluster. For a remote cluster, push it to a registry and update the image in `k8s.yaml`. Create the database password Secret and apply the manifest:

```powershell
kubectl create secret generic institute-db-secret --from-literal=DB_PASSWORD="choose-a-strong-password"
kubectl apply -f k8s.yaml
```

Check deployment status and forward the service port to your machine:

```powershell
kubectl get deployments,pods,services
kubectl port-forward service/institute-service 8080:8080
```

Open <http://localhost:8080/> to use the dashboard. The password Secret is managed separately from the manifest.

## Tests

Run the test suite:

```powershell
mvn test
```

Tests use an in-memory H2 database and do not require PostgreSQL.
