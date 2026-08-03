# Appointments

Backend for a service-booking system (salon/service business): clients, masters, services, service groups, appointments, and master holidays (days off). REST API built with Spring Boot, deployed to Google Cloud Run via Docker.

## Stack

- **Java 17**, **Spring Boot 3.5.3**
- Spring Web (REST), Spring Data JPA
- MySQL (`mysql-connector-j`)
- Lombok
- Maven (`mvnw`)
- Docker + Google Cloud Run (`cloudbuild.yaml`)

## Project structure

```
src/main/java/com/example/appointments/
├── controller/   # REST controllers (talk to repositories directly, no service layer)
├── entity/       # JPA entities
├── dto/          # Request/response DTOs
├── repository/   # Spring Data JPA repositories
└── config/       # CORS and other web configuration
```

The architecture is intentionally simple: there is no service/business layer — controllers work with repositories directly. Keep this in mind when adding new logic.

## Entities

| Entity | Description |
|---|---|
| `Master` | A master/stylist who provides services (`Service`), belongs to service groups, and has days off (`Holiday`) |
| `Service` | A service (name, duration `period`, price), can be provided by multiple masters |
| `GroupService` | A service group (category) |
| `Client` | A client (name, phone, email, google id) |
| `Appointment` | A client's booking with a master for a service(s) at a specific time (`datatime`) |
| `Holiday` | A period when a master is unavailable |

## API endpoints

### `/api/appointments`
- `POST /api/appointments` — create a booking (creates the client if they don't exist yet)
- `GET /api/appointments/available?masterId=&serviceIds=` — available slots for a master. `serviceIds` (optional, repeatable) is used to compute the total duration of the requested services, so slots that wouldn't fit before the next booking or closing time are excluded. Falls back to a 30-minute default slot if omitted.

### `/api/masters`
- `GET /api/masters` — all masters
- `GET /api/masters/group/{groupId}` / `/group/{groupId}/short`
- `GET /api/masters/service/{serviceId}` / `/service/{serviceId}/short`

### `/api/services`
- `GET /api/services`, `GET /api/services/list`
- `GET /api/services/group/{groupId}`

### `/api/groups`
- `GET /api/groups`, `GET /api/groups/list`, `GET /api/groups/{id}`

### `/api/clients`
- `GET /api/clients` — all clients 🔒 requires `X-Admin-Key` (full PII, admin-only)
- `GET /api/clients/search` — search for a client by mobile (public)
- `POST /api/clients` — create a client (public, used by the booking form)

### `/api/holidays`
- `GET /api/holidays`, `GET /api/holidays/master/{id}`
- `POST /api/holidays`, `DELETE /api/holidays/{id}`

### `/api/admin` — administration
Full CRUD for appointments, services, masters, clients, and groups (`GET`/`POST`/`DELETE` under `/api/admin/**`).

> 🔒 **Protected.** Every request under `/api/admin/**` must include the header `X-Admin-Key: <ADMIN_API_KEY>`. Requests without a matching key get `401 Unauthorized`. See [`AdminApiKeyFilter`](src/main/java/com/example/appointments/security/AdminApiKeyFilter.java).

## Known issues / in progress

- `AdminController`'s entity-bound endpoints (`saveAppointment`, `saveService`, `saveMaster`, `saveGroup`) bind raw JPA entities directly from the request body (mass assignment) — a caller can set fields like `id` that shouldn't be client-controlled.
- The `Appointment` entity/column/repository methods still use the misspelled field name `datatime` (should be `datetime`). Left as-is deliberately — renaming it means a DB column migration, which is riskier than the rest of the cleanup and needs its own separate change.

### Input validation & error handling

`POST /api/appointments` and `POST /api/holidays` validate their request bodies with Bean Validation (`@Valid` + `jakarta.validation` annotations on `AppointmentCreateDto` / `HolidayCreateDto`). A [`GlobalExceptionHandler`](src/main/java/com/example/appointments/exception/GlobalExceptionHandler.java) turns the common failure cases into clean JSON responses instead of raw 500s:

- Validation failures → `400` with a `fields` map of which field failed and why
- Unparseable dates/times → `400`
- Deleting a non-existent id (`deleteById`) → `404`
- "Not found" lookups (`master`, `service`, `group`) now throw `ResponseStatusException` directly → proper `404` instead of a generic `RuntimeException`/`500`

Note: `AdminController`'s entity-bound endpoints (`saveAppointment`, `saveService`, `saveMaster`, `saveGroup`) still accept raw JPA entities as the request body and are not yet covered by DTO-level validation.

### Booking conflict detection

`POST /api/appointments` checks for overlapping bookings for the same master before saving: it compares the new appointment's `[start, start + total service duration]` window against the master's existing appointments for that day, and rejects the request with `409 Conflict` if they overlap. The conflict query also takes a pessimistic write lock on the master's same-day appointments for the duration of the transaction, so two concurrent booking requests can't both slip past the check (see [`AppointmentRepository.lockByMasterIdAndDatatimeBetween`](src/main/java/com/example/appointments/repository/AppointmentRepository.java)).

## Git workflow

### Clone the repo

```bash
git clone https://github.com/lazoriks/appointmentsSpring.git
cd appointmentsSpring
```

### Push changes

```bash
git checkout -b feature/my-change   # create a branch for your change
git add <files>                     # stage specific files (avoid `git add .`)
git commit -m "Describe the change"
git push -u origin feature/my-change
```

Then open a pull request into `main` on GitHub. Avoid pushing directly to `main` — go through a PR so changes get reviewed before deploy.

To pull the latest changes from `main` into your local checkout:

```bash
git checkout main
git pull
```

## Running locally

```bash
./mvnw spring-boot:run
```

Requires the environment variables below (or a local `application-local.properties`).

### Environment variables (`application.properties`)

```properties
spring.application.name=appointments

### Database connection
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

### JPA / Hibernate
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:none}
spring.jpa.database-platform=${SPRING_JPA_DATABASE_PLATFORM}
spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:true}
spring.jpa.properties.hibernate.format_sql=${SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL:true}

### Let Render control the port for test deploy and endpoint
server.port=${PORT:8080}

### Admin API key (required header X-Admin-Key on /api/admin/**)
admin.api-key=${ADMIN_API_KEY:}

### CORS (comma-separated list of allowed origins)
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://glamlimerick.com,http://localhost:3000}
```

CORS is configured in a single place ([`CorsGlobalConfig`](src/main/java/com/example/appointments/config/CorsGlobalConfig.java)) for `/api/**`, reading its allowed origins from `app.cors.allowed-origins`. There's no `@CrossOrigin` on individual controllers anymore — add a new frontend origin by updating `CORS_ALLOWED_ORIGINS`, not by editing code.

In Render (Dashboard → Environment), create these variables:

| Key | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://appointmentdb.ch8kskc0cuv1.eu-north-1.rds.amazonaws.com:3306/db_group_service` |
| `SPRING_DATASOURCE_USERNAME` | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | `***` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `none` |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.MySQL8Dialect` |
| `SPRING_JPA_SHOW_SQL` | `true` |
| `SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL` | `true` |
| `ADMIN_API_KEY` | a long random secret — required for `/api/admin/**` (sent as the `X-Admin-Key` header) |
| `CORS_ALLOWED_ORIGINS` | comma-separated list, e.g. `https://glamlimerick.com,http://localhost:3000` |

## Deployment

### Google Cloud Run + Spring Boot

**1. Make `mvnw` executable locally and commit it**

Use Git Bash or WSL on Windows, then:

```bash
chmod +x mvnw
git add mvnw
git commit -m "Make mvnw executable"
git push
```

If you can't do this on your machine, a quick script or guide can be generated to fix it.

**2. Required tools**

cmd, PowerShell, or Windows Terminal:

```bash
docker --version
gcloud --version
```

- Docker Desktop 👉 https://www.docker.com/products/docker-desktop/
- Google Cloud SDK 👉 https://cloud.google.com/sdk/docs/install

**3. Build and deploy the Dockerfile**

```bash
docker build -t gcr.io/keen-ascent-465022-s5/appointmentspring:latest .
docker push gcr.io/keen-ascent-465022-s5/appointmentspring:latest
```

Production: https://appointmentspring-206160864813.us-central1.run.app/api/clients

Automated deployment is configured via `cloudbuild.yaml` (Cloud Build → Cloud Run, region `us-central1`).
