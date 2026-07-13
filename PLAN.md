# RAG Chat Storage Microservice — Implementation Plan

## Context

The user is preparing for a Java backend developer interview and has a case study brief ("Backend Developer Case Study 25.docx") asking for a production-ready backend microservice that stores chat histories from a RAG chatbot: sessions (create/rename/favorite/delete) and messages (add/retrieve with sender, content, optional retrieved context), protected by API-key auth, rate-limited, centrally logged/error-handled, Dockerized, with a README. Bonus items requested: health checks, Swagger/OpenAPI, dockerized pgAdmin, unit tests, CORS config, pagination.

The project directory is currently empty (only the docx brief). This is a greenfield build. Confirmed decisions: **Maven**, **PostgreSQL**, **full bonus scope**, and chat sessions carry a **`userId`** field (no full login system — API key is the only auth mechanism, `userId` is just a caller-supplied data field). The goal is idiomatic, defensible Spring Boot code that demonstrates good judgment in an interview setting — not a padded enterprise system.

## Tech Stack

Java 17, Spring Boot 3.x, Maven, PostgreSQL 16, Flyway, Spring Data JPA, Spring Security (for the API-key filter chain only, no login), Bucket4j (in-memory rate limiting), springdoc-openapi, Spring Boot Actuator, Lombok, JUnit 5 + Mockito + AssertJ, Docker + Docker Compose (app + postgres + pgadmin).

## Package layout (feature-based)

`com.nagarro.ragchat` with `session/` (entity, repo, service, controller, dto/), `message/` (same), and cross-cutting `common/` (exceptions, error DTO, paged DTO, correlation-id logging filter), `security/` (API key filter + entry point), `ratelimit/` (Bucket4j filter), `config/` (SecurityConfig, CorsConfig, OpenApiConfig).

## Key design decisions

- **Cascade delete at the DB level, not JPA**: `ChatMessage` has a unidirectional `@ManyToOne` to `ChatSession` (no bidirectional `@OneToMany`). The Flyway migration defines `session_id` FK with `ON DELETE CASCADE`, so deleting a session is one `DELETE` statement — Postgres cascades the messages without Hibernate ever loading them into memory. Worth explicitly mentioning in the interview.
- **Flyway with `ddl-auto=validate`** — schema is versioned SQL, not Hibernate auto-generation.
- **`context` field as JSONB** (`JsonNode` type via `@JdbcTypeCode(SqlTypes.JSON)`) — arbitrary structured RAG retrieval data, no rigid shape forced.
- **DTOs are Java records** with static `from(entity)` factories — no MapStruct (unnecessary for 2 entities).
- **Explicit `PagedResponse<T>` DTO**, not raw Spring Data `Page<T>` — stable, documented API contract.
- **API-key auth via Spring Security filter chain** (`OncePerRequestFilter` + custom `AuthenticationEntryPoint`), key read from `API_KEY` env var, constant-time comparison, fails fast at startup if unset.
- **Rate limiting via Bucket4j, in-memory, keyed by client IP** (not by the shared API key, since all callers share one key). No Redis — out of scope for a single-instance case study; call out Caffeine/Redis as the noted future-hardening step.
- **Filters (API key, rate limit) write their own JSON error bodies** (reusing the shared `ErrorResponse` record) since they run before `DispatcherServlet` and `@RestControllerAdvice` never sees them.
- **CORS configured via Spring Security's `CorsConfigurationSource`** (not a separate `WebMvcConfigurer`), so preflight `OPTIONS` requests aren't blocked before reaching MVC.
- Actuator health/info and Swagger UI/OpenAPI JSON are `permitAll` (excluded from API-key auth and rate limiting) so orchestrators and reviewers can reach them freely.

## API surface

**Sessions** — `/api/v1/sessions`
- `POST /` → create session (userId, title) → 201
- `GET /?userId=&page=&size=&sort=` → paginated list → 200
- `GET /{sessionId}` → 200 / 404
- `PATCH /{sessionId}` → partial update (title and/or favorite) → 200 / 404
- `DELETE /{sessionId}` → 204 / 404

**Messages** — `/api/v1/sessions/{sessionId}/messages`
- `POST /` → add message (sender, content, optional context) → 201 / 404 if session missing
- `GET /?page=&size=&sort=createdAt,asc` → paginated history → 200 / 404

All protected endpoints require header `X-API-Key`. Errors return a consistent `ErrorResponse` JSON (timestamp, status, error, message, path, requestId).

## Implementation order

1. **Scaffold** — `pom.xml` (web, data-jpa, validation, security, actuator, postgresql, flyway-core + flyway-database-postgresql, springdoc-openapi-starter-webmvc-ui, bucket4j, lombok, spring-dotenv, spring-boot-starter-test), package skeleton, `.gitignore`.
2. **Config** — `application.yml` + `application-dev.yml` + `application-docker.yml` (env-var-driven via `${VAR:default}`), `logback-spring.xml` with `%X{requestId}`, `.env.example`.
3. **Flyway migration** `V1__init_schema.sql` — `chat_session` and `chat_message` tables, indexes on `user_id` and `session_id`, FK with `ON DELETE CASCADE`.
4. **Entities** — `ChatSession`, `SenderType` enum, `ChatMessage` (JSONB `context` field).
5. **Repositories** — `ChatSessionRepository` (`findByUserId` paginated), `ChatMessageRepository` (`findBySessionId` paginated).
6. **Common building blocks** — `ErrorResponse`, `PagedResponse<T>`, `SessionNotFoundException`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `CorrelationIdFilter`.
7. **DTOs** — `CreateSessionRequest`, `UpdateSessionRequest`, `ChatSessionResponse`; `CreateMessageRequest`, `ChatMessageResponse` — with bean validation annotations.
8. **Services** — `ChatSessionService` (create/list/get/update/delete, `@Transactional` where mutating), `ChatMessageService` (addMessage/getMessages, validates parent session exists, depends on both repositories directly — not on `ChatSessionService`).
9. **Controllers** — `ChatSessionController`, `ChatMessageController` — thin, delegate to services, no try/catch (handled centrally).
10. **API-key auth** — `ApiKeyProperties`, `ApiKeyAuthFilter`, `ApiKeyAuthenticationEntryPoint`, `SecurityConfig` (stateless, CSRF disabled, permitAll for actuator/swagger, filter ordering: rate-limit → api-key → rest of chain).
11. **Rate limiting** — `RateLimitProperties`, `RateLimitFilter` (Bucket4j, per-IP bucket map, 429 + `Retry-After` on exhaustion, skips actuator paths).
12. **CORS** — `CorsConfig` bean wired into `SecurityConfig`, origins from `ALLOWED_ORIGINS` env var.
13. **OpenAPI/Swagger** — `OpenApiConfig` with API-key `SecurityScheme` so Swagger UI's Authorize button works end-to-end.
14. **Actuator** — expose `health,info`, verify Postgres health indicator auto-wires.
15. **Pagination** — already threaded through via `Pageable` + `PagedResponse` mapping in services; set default/max page size in `application.yml`.
16. **Unit tests** — `ChatSessionServiceTest` and `ChatMessageServiceTest` (Mockito, no Spring context), covering happy paths and not-found exceptions per the plan above.
17. **Docker** — multi-stage `Dockerfile` (Maven build stage → slim JRE runtime, non-root user, Actuator healthcheck), `docker-compose.yml` (app + postgres + pgadmin, named volumes, env passthrough), `.dockerignore`.
18. **README.md** — setup/run instructions (Docker and local), env var table, API reference, auth example, rate-limit/pagination/error-format examples, pgAdmin access instructions, test instructions, design-decisions section, future-improvements section.

## Verification

- `mvn test` — all unit tests pass.
- `docker compose up --build` — app, postgres, and pgadmin start; app healthcheck passes.
- Manual smoke test via `curl`/Swagger UI (`http://localhost:8080/swagger-ui.html`): create a session, add a message with context, list sessions by userId, rename a session, mark favorite, paginate messages, delete a session and confirm messages are gone, confirm a request without `X-API-Key` returns 401, confirm exceeding the rate limit returns 429.
- `GET /actuator/health` returns UP with the Postgres indicator.
- Confirm every env var used in YAML/compose is documented in `.env.example`.
