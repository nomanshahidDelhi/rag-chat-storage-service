# RAG Chat Storage Microservice

A backend microservice for storing and managing chat histories produced by a RAG
(Retrieval-Augmented Generation) chatbot: chat sessions and the messages within them,
including any retrieved context attached to an assistant reply. This service only
stores/retrieves chat history — it does not call an LLM or perform retrieval itself.

## Features

**Core**
- Create, list (by user), fetch, rename, favorite/unfavorite, and delete chat sessions.
- Add messages to a session (sender, content, optional retrieved context) and retrieve message history.
- Deleting a session cascades to its messages at the database level.
- API key authentication on all `/api/**` endpoints.
- Rate limiting per client IP.
- Centralized logging (request correlation IDs) and global error handling.
- Dockerized for local setup.

**Bonus**
- Health check endpoints (Spring Boot Actuator).
- Swagger / OpenAPI documentation.
- Dockerized pgAdmin for browsing the database.
- Unit tests for service-layer business logic.
- CORS configuration.
- Pagination on session and message listing endpoints.

## Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3 (Web, Data JPA, Security, Validation, Actuator) |
| Build tool | Maven |
| Database | PostgreSQL 16, versioned with Flyway |
| Auth | API key via a Spring Security filter chain |
| Rate limiting | Bucket4j (in-memory, per client IP) |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, AssertJ |
| Containerization | Docker, Docker Compose |

## Package structure

```
com.nagarro.ragchat
├── session/            # ChatSession entity, repository, service, controller, dto/
├── message/             # ChatMessage entity, repository, service, controller, dto/
├── security/            # API key auth filter, entry point, properties
├── ratelimit/            # Bucket4j rate-limit filter, properties
├── config/               # SecurityConfig, CorsConfig, OpenApiConfig
└── common/               # ErrorResponse, PagedResponse, exceptions, correlation-id filter
```

Packages are grouped by feature (`session`, `message`) rather than by layer, since each
feature is small and self-contained; cross-cutting infrastructure lives in its own
`security` / `ratelimit` / `config` / `common` packages.

## Prerequisites

- Docker and Docker Compose (recommended path — no local Java/Maven/Postgres needed).
- Optionally, Java 17 and Maven if you want to run outside Docker.

## Getting started (Docker)

```bash
cp .env.example .env
# edit .env if you want, at minimum change API_KEY for anything beyond local testing

docker compose up --build
```

This starts three containers:
- `app` — the microservice, on `http://localhost:8080`
- `postgres` — PostgreSQL, on `localhost:5432`
- `pgadmin` — pgAdmin, on `http://localhost:5050`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running locally without Docker

1. Start just the database: `docker compose up postgres`
2. `cp .env.example .env` and adjust `POSTGRES_*` / `API_KEY` as needed. The app auto-loads
   `.env` via `spring-dotenv` when run with `mvn spring-boot:run`.
3. `mvn spring-boot:run` (uses the `dev` profile by default, pointing at `localhost:5432`).
4. Start pgAdmin if wants to see the database GUI: `docker compose up -d pgadmin`
## Environment variables

| Variable | Description | Default |
|---|---|---|
| `API_KEY` | Shared secret required in the `X-API-Key` header on every `/api/**` request | *(required, no default)* |
| `POSTGRES_DB` | Database name | `ragchat` |
| `POSTGRES_USER` | Database user | `ragchat` |
| `POSTGRES_PASSWORD` | Database password | `ragchat` |
| `POSTGRES_PORT` | Host port mapped to Postgres | `5432` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `docker`) | `docker` (compose) / `dev` (local) |
| `APP_PORT` | Host port mapped to the app | `8080` |
| `ALLOWED_ORIGINS` | Comma-separated CORS allowed origins | `http://localhost:3000` |
| `RATE_LIMIT_CAPACITY` | Requests allowed per client IP per refill window | `60` |
| `RATE_LIMIT_REFILL_SECONDS` | Refill window length, in seconds | `60` |
| `PGADMIN_DEFAULT_EMAIL` | pgAdmin login email | `admin@example.com` |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin login password | `admin` |
| `PGADMIN_PORT` | Host port mapped to pgAdmin | `5050` |

See `.env.example` for a ready-to-copy file.

## Authentication

Every `/api/**` endpoint requires an `X-API-Key` header matching the `API_KEY` env var.
Requests without a valid key receive `401 Unauthorized`.

```bash
curl -H "X-API-Key: changeme-super-secret-key" http://localhost:8080/api/v1/sessions?userId=demo-user
```

`/actuator/health`, `/actuator/info`, and the Swagger UI/OpenAPI JSON are public.

## API reference

Base path: `/api/v1`

### Sessions — `/sessions`

| Method | Path | Description |
|---|---|---|
| POST | `/sessions` | Create a session. Body: `{ "userId": "...", "title": "..." (optional) }` |
| GET | `/sessions?userId=&page=&size=&sort=` | Paginated list of a user's sessions |
| GET | `/sessions/{sessionId}` | Fetch a single session |
| PATCH | `/sessions/{sessionId}` | Partial update. Body: `{ "title": "..." }` and/or `{ "favorite": true/false }` |
| DELETE | `/sessions/{sessionId}` | Delete a session and all its messages |

### Messages — `/sessions/{sessionId}/messages`

| Method | Path | Description |
|---|---|---|
| POST | `/sessions/{sessionId}/messages` | Add a message. Body: `{ "sender": "USER"\|"ASSISTANT", "content": "...", "context": {...} (optional) }` |
| GET | `/sessions/{sessionId}/messages?page=&size=&sort=createdAt,asc` | Paginated message history |

Full request/response schemas are available in Swagger UI at `/swagger-ui.html` (click
**Authorize** and paste your API key to try requests from the browser).

## Rate limiting

Each client IP gets a token bucket of `RATE_LIMIT_CAPACITY` requests, refilled every
`RATE_LIMIT_REFILL_SECONDS`. Exceeding it returns `429 Too Many Requests` with a
`Retry-After` header:

```json
{
  "timestamp": "2026-07-13T10:15:30Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded, try again later",
  "path": "/api/v1/sessions",
  "requestId": "..."
}
```

## Pagination

List endpoints accept standard Spring Data query params: `page` (0-indexed), `size`
(default 20, max 100), and `sort` (e.g. `sort=updatedAt,desc`). Responses are wrapped:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "last": true
}
```

## Error format

All errors (400/401/404/429/500) return a consistent body:

```json
{
  "timestamp": "2026-07-13T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Chat session not found: 3f2c...",
  "path": "/api/v1/sessions/3f2c...",
  "requestId": "..."
}
```

## Health check

`GET /actuator/health` — reports application and database status; used by the Docker
healthcheck.

## pgAdmin

Open `http://localhost:5050`, log in with `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD`,
then **Add New Server**:
- Host: `postgres`
- Port: `5432`
- Username / Password / Database: your `POSTGRES_*` values

## Running tests

```bash
mvn test
```

Service-layer business logic (`ChatSessionService`, `ChatMessageService`) and the API key
filter are covered with Mockito-based unit tests (no Spring context, no database).

## Design decisions

- **DB-level cascade delete**: `chat_message.session_id` has `ON DELETE CASCADE` in the
  Flyway migration, and `ChatMessage` only holds a unidirectional `@ManyToOne` to
  `ChatSession`. Deleting a session is a single `DELETE` statement — Postgres cascades
  the messages without Hibernate loading any of them into memory.
- **Flyway over `ddl-auto=update`**: schema is versioned SQL; Hibernate only validates
  the mapping at startup.
- **API key auth via Spring Security's filter chain**, not a bare unmanaged filter —
  gives a standard `SecurityContext`, a clean `AuthenticationEntryPoint` for consistent
  401 bodies, and first-class CORS integration.
- **Bucket4j, in-memory, per-IP** rate limiting — a distributed limiter (Redis-backed)
  would be over-engineering for a single-instance service.
- **Explicit `PagedResponse<T>` DTO** instead of returning Spring Data's `Page<T>`
  directly — keeps the JSON response shape a stable, documented contract.
- **`context` stored as JSONB** (`JsonNode`) — arbitrary structured RAG retrieval data
  (chunks, sources, scores) without forcing a rigid shape.
