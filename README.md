# loan-api

![CI](https://github.com/HemHarshith/loan-api/actions/workflows/ci.yml/badge.svg)

REST API for managing loan applications, built with Java 21 and Spring Boot. Officers submit and view applications; admins review them. Data lives in PostgreSQL with versioned schema migrations, and every endpoint except login, registration and health checks requires a JWT.

## Tech stack

- Java 21, Spring Boot 4, Maven
- Spring Web (REST), Bean Validation
- PostgreSQL, Spring Data JPA (Hibernate), Flyway migrations
- Spring Security with JWT (OAuth2 Resource Server), BCrypt password hashing
- JUnit 5, Mockito, MockMvc (`@WebMvcTest`)
- Docker and Docker Compose, GitHub Actions CI

## Endpoints

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public (creates an OFFICER) |
| POST | `/api/auth/login` | Public (returns a JWT) |
| GET | `/api/ping`, `/actuator/health` | Public |
| POST | `/api/loan-applications` | Any logged-in user |
| GET | `/api/loan-applications`, `/api/loan-applications/{id}` | Any logged-in user |
| PATCH | `/api/loan-applications/{id}/status` | ADMIN |
| DELETE | `/api/loan-applications/{id}` | ADMIN |

Send the token as `Authorization: Bearer <token>`. Errors come back as JSON: `400` with a per-field message for invalid input, `401` without a valid token, `403` for the wrong role, `404` for an unknown id.

## Run with Docker

Requires Docker Desktop. Set two environment variables, then start the app and its own PostgreSQL:

```powershell
$env:DB_PASSWORD = "choose-a-database-password"
$env:JWT_SECRET  = "choose-a-random-secret-of-at-least-32-characters"
docker compose up --build
```

The API is on `http://localhost:8080` and the database on port `5433`. Flyway creates the tables on first start.

## Run locally (without Docker)

Needs JDK 21 and a PostgreSQL database named `loanapi` on port 5432.

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET  = "a-random-secret-of-at-least-32-characters"
.\mvnw spring-boot:run
```

## Tests

```powershell
.\mvnw test "-Dtest=LoanApplicationServiceTest,LoanApplicationControllerTest"
```

- Service unit tests (JUnit 5, Mockito): status rules, not-found handling, no `save()` on updates.
- Controller tests (`@WebMvcTest`): validation errors, 401/403/404, role rules on the real security config.
- CI also runs the full-context test against a real PostgreSQL service.

## Design decisions

- **Layers:** controller, service, repository. Storage was swapped from an in-memory map to PostgreSQL without touching the controller.
- **Separate request, response and entity types:** clients cannot set the id or status, and table changes do not silently change the API.
- **Flyway owns the schema** and Hibernate only validates it (`ddl-auto=validate`).
- **Stateless JWT auth:** passwords stored as BCrypt hashes; the signing secret comes from an environment variable, never from the repo.
- **Money as `BigDecimal` / `NUMERIC(15,2)`** to avoid rounding errors.

## Known limitations

- Roles are enforced on status changes and deletes only; any logged-in user can read all loans (no ownership check).
- Registration is open and always creates an OFFICER; admins are promoted manually in the database.
- No refresh tokens, token revocation, rate limiting or pagination yet.
- No automated end-to-end role/token test; the JWT role mapping was verified manually.
- Not deployed anywhere yet.