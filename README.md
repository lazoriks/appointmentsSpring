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

### `/api/admin/site-images` — photos shown on the WordPress home page
- `GET /api/admin/site-images` — the 14 fixed slots (7 category sections × 2 photos) with their permanent public URLs
- `POST /api/admin/site-images/{slot}` — multipart upload (field name `file`) replacing that slot's photo

The salon manager has no WordPress access, so category photos are served from a public Google Cloud Storage bucket instead of the WP media library. Each slot has a permanent URL (`…/categories/nails-1`) that the home page links to once; an upload overwrites the object behind it, so the page markup never changes. Objects are stored with `Cache-Control: public, max-age=300`, so a replacement appears on the site within about five minutes.

Uploads are validated: slot must be one of the known 14, type must be JPEG/PNG/WebP, size ≤ 5 MB. Being under `/api/admin/**`, they're already covered by `AdminApiKeyFilter`. See [`SiteImageController`](src/main/java/com/example/appointments/controller/SiteImageController.java).

### `/api/auth/login` — admin login
`POST /api/auth/login` with `{ "username": "...", "password": "..." }`, checked against `ADMIN_USERNAME`/`ADMIN_PASSWORD`. On success returns `{ "apiKey": "<ADMIN_API_KEY>" }` for the frontend to store and send as `X-Admin-Key` on subsequent admin requests — so the human-facing credential is a normal username/password, not the raw key. See [`AuthController`](src/main/java/com/example/appointments/controller/AuthController.java).

## Booking notifications

When a booking is created, three messages go out:

| To | Channel | Contents |
|---|---|---|
| The client | Email | Confirmation: date and time, services, beautician, salon address, phone to call for changes |
| The salon | Email (`SALON_EMAIL`) | Alert with the client's name, mobile, email, services, beautician and total |
| The salon | Telegram group | The same alert, short form |

**Clients get email rather than Telegram** because a bot can't message someone who hasn't started a chat with it — there's no way to reach a person who just filled in a web form.

### How it's delivered

Sending inline would mean a slow SMTP server delays the customer's booking, and a failing one could fail it outright. Instead this uses a **transactional outbox**:

1. `AppointmentController` saves the appointment and, **in the same transaction**, writes one `notification_outbox` row per message. A booking therefore can't exist without its notifications being queued, and notifications can't be queued for a booking that rolled back.
2. After the transaction commits, `BookingCreatedListener` tries to deliver them immediately (async, off the request thread), so the salon usually hears within seconds.
3. Anything that fails — or never runs, because Cloud Run shut the instance down mid-send — stays `PENDING` and is retried by `POST /api/admin/notifications/dispatch`.

That endpoint exists because **Cloud Run scales to zero**: no CPU is scheduled between requests, so a `@Scheduled` method would simply never fire. A **Cloud Scheduler** job (`notifications-dispatch`, `europe-west1`, every 10 minutes) calls it with the `X-Admin-Key` header, which wakes the service and drains the queue.

Each row is sent in its own transaction, records `attempts` and `last_error`, and is marked `FAILED` after 5 tries so a bad address can't be retried forever.

```
GET  /api/admin/notifications/status    -> {"pending":0,"failed":0}
POST /api/admin/notifications/dispatch  -> {"sent":2,"stillPending":0,"failed":0}
```

### Configuration

Every channel is optional — leave its variables blank and that channel is skipped. Bookings keep working either way; the service also starts fine with nothing configured.

| Variable | Notes |
|---|---|
| `MAIL_HOST` / `MAIL_PORT` | e.g. `smtp.gmail.com` / `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | For Gmail this must be an **App Password** (needs 2-Step Verification), not the account password |
| `SALON_EMAIL` | Where the salon's copy goes |
| `TELEGRAM_BOT_TOKEN` | From [@BotFather](https://t.me/BotFather) |
| `TELEGRAM_CHAT_ID` | The salon group's chat id — add the bot to the group, send a message, then read it from `https://api.telegram.org/bot<TOKEN>/getUpdates`. Group ids are negative |
| `SALON_NAME` / `SALON_ADDRESS` / `SALON_PHONE` | Shown in the messages; sensible defaults are built in |

## Problems & Solutions

### 1. N+1 queries made admin list endpoints slow (~13s for 89 services)

**Problem:** `GET /api/admin/services` took ~13 seconds to return 89 rows. `Service.masters` is a lazy `@ManyToMany`; Jackson touching it while serializing every row fired one extra query per service. Each of those was a round trip from the Cloud Run backend (then `us-central1`) to the RDS database (`eu-north-1`) — 89 unnecessary cross-region queries for data no frontend even reads.

**Solution:**
- [`Service.masters`](src/main/java/com/example/appointments/entity/Service.java) is never used by any frontend, so it's now `@JsonIgnore`'d — the extra query disappears entirely for this field.
- [`Master.services`](src/main/java/com/example/appointments/entity/Master.java) and [`Appointment.services`](src/main/java/com/example/appointments/entity/Appointment.java) *are* used (master-edit's service checklist, an appointment's booked-services list), so they can't be dropped the same way — added `@BatchSize(50)` instead, turning N+1 lazy loads into ~1 batched `IN (...)` query per list load.
- Combined with moving the region to `europe-north1` (see [Deployment](#deployment)), `GET /api/admin/services` went from ~13.4s to ~0.6s.

### 2. Double-booking race condition

**Problem:** Two clients booking the same master for an overlapping slot at the same moment could both pass the "is this slot free?" check before either write committed, creating two appointments for the same master at the same time.

**Solution:** `POST /api/appointments` runs in a `@Transactional` block and loads the master's same-day appointments via [`AppointmentRepository.lockByMasterIdAndDatatimeBetween`](src/main/java/com/example/appointments/repository/AppointmentRepository.java), which uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` — a `SELECT ... FOR UPDATE` that locks those rows for the transaction's duration. A concurrent request for the same master/day blocks until the first transaction commits or rolls back, so it always sees the just-created booking when it re-checks for overlap and correctly gets rejected with `409 Conflict` instead of racing past the check.

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

### Site photos (uploaded from the admin panel, shown on the WordPress home page)
app.site-images.bucket=${SITE_IMAGES_BUCKET:glamlimerick-site-images}
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB

### CORS (comma-separated list of allowed origins)
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://glamlimerick.com,http://localhost:3000}
```

The bucket is `glamlimerick-site-images` (region `europe-north1`, uniform access). `allUsers` has `roles/storage.objectViewer` so the website can read the photos, and the Cloud Run service account has `roles/storage.objectAdmin` so uploads can write them. Authentication uses the runtime's default credentials — no key file.

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
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | credentials `POST /api/auth/login` checks before handing back `ADMIN_API_KEY` |
| `SITE_IMAGES_BUCKET` | GCS bucket for the home page photos (defaults to `glamlimerick-site-images`) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP for booking emails — see [Booking notifications](#booking-notifications) |
| `SALON_EMAIL` | Where the salon's copy of each booking goes |
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` | Bot and group chat for booking alerts |
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

Production: https://appointmentspring-206160864813.europe-north1.run.app/api/clients

Automated deployment is configured via `cloudbuild.yaml` (Cloud Build → Cloud Run, region `europe-north1` — chosen to sit next to the RDS database in AWS `eu-north-1` instead of across the Atlantic from it).
