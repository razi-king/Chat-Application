# Learning Guide

A path through the project from zero to "I can explain and change any part of it".
For every topic: **Concept · In this project · Why needed · Without it · Common mistakes · Interview questions.**
Open the referenced files side by side — the code is the textbook.

Suggested reading order of code: `ChatAppBackendApplication` → `controler/RoomControler` → `service/impl/ChatServiceImpl` → `repository/MessageRepository` → `entity/Message` → `exception/RestResponseExceptionHandler` → `security/JwtAuthenticationFilter` → `config/WebSocketConfig` → `chat-frontend/component/chat/ChatPage.tsx`.

---

## 1. Project structure
* **Concept:** packages group code by technical role; a reader should guess where something lives.
* **In this project:** `controler/ dto/ entity/ enums/ exception/ mapper/ payload/ repository/ security/ service/ service/impl/ config/ util/` (backend), `app/ component/ context/ lib/ services/ types/` (frontend). See README §10.
* **Why:** separation of concerns — HTTP, rules, persistence, security each in their own place.
* **Without it:** a 2 000-line controller where a bug fix in validation breaks persistence.
* **Mistakes:** putting business logic in controllers; one `utils` package that becomes a junk drawer.
* **Interview:** *"Walk me through your project structure."* *"Package by layer vs by feature — trade-offs?"* (by layer: easy to learn; by feature: scales better for big teams.)

## 2. Java concepts used
* **Concept:** records, enums with fields, interfaces, generics, lambdas/streams, `Optional`, `java.time.Instant`, concurrency utilities.
* **In this project:** `AuthUser` is a **record** (immutable principal); `ErrorCode`/`MemberRole` are **enums with fields and behaviour** (`atLeast`); `Response<T>`/`PageResponse<T>` are **generics**; `Optional.orElseThrow` in `AccessGuard`; `AtomicLong`/`ConcurrentHashMap` in `PresenceServiceImpl`; `Instant` everywhere (UTC, no timezone bugs).
* **Why:** less boilerplate, fewer null bugs, thread safety.
* **Without it:** `null` checks everywhere, `Date` timezone confusion, mutable shared state.
* **Mistakes:** `Optional.get()` without check; using `LocalDateTime` for events across timezones; `HashMap` shared across threads.
* **Interview:** *"Record vs class?"* *"Why `Instant` and not `LocalDateTime` for message time?"* *"What does `computeIfPresent` guarantee?"*

## 3. Spring Boot concepts
* **Concept:** auto-configuration, starters, `application.properties`, component scanning, profiles.
* **In this project:** starters in `pom.xml` (web, data-mongodb, security, websocket, data-redis, validation); config values with defaults like `${JWT_SECRET:dev-only...}`; `@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)` because we provide our own authentication.
* **Why:** a working server with almost no setup code.
* **Without it:** manual Tomcat, Jackson, Mongo client and thread pool wiring.
* **Mistakes:** hard-coding secrets; not knowing what auto-config did (use `--debug`).
* **Interview:** *"How does Spring Boot auto-configuration work?"* *"How do you externalise configuration?"*

## 4. Dependency Injection
* **Concept:** objects receive their collaborators instead of creating them (Inversion of Control container).
* **In this project:** constructor injection via Lombok `@RequiredArgsConstructor` + `private final` fields (e.g. `ChatServiceImpl`). The original code used field `@Autowired`; it was changed.
* **Why:** testability — `ChatServiceImplTest` passes mocks through the constructor; immutability of dependencies; the compiler shows missing dependencies.
* **Without it:** `new MessageRepository()` inside services → impossible to test or swap.
* **Mistakes:** field injection; circular dependencies (we avoided `ChatService` ↔ `RoomService` cycles by having only Room/Server depend on Chat).
* **Interview:** *"Constructor vs field injection?"* *"What is a bean scope?"* *"How would you break a circular dependency?"*

## 5. REST API
* **Concept:** resources + HTTP verbs + status codes, stateless requests.
* **In this project:** `/api/v1/rooms/{id}/messages` (GET history, POST send), `/messages/{id}/pin`, versioned path, consistent statuses (API-DOCUMENTATION.md).
* **Why:** predictable, cacheable, tool-friendly (Swagger).
* **Without it:** RPC-style `/doEverything?action=...` endpoints nobody can document.
* **Mistakes:** 200 with `{error: ...}`; verbs in URLs; leaking database ids of internal relations.
* **Interview:** *"PUT vs PATCH vs POST?"* *"Which operations are idempotent?"* *"401 vs 403?"*

## 6. DTOs
* **Concept:** dedicated classes for what crosses the API boundary.
* **In this project:** `dto/request/*` (input with validation), `dto/response/*` (output), `dto/event/*` (WebSocket), `Response<T>` envelope; mappers convert entities.
* **Why:** never expose `passwordHash`; the API contract stays stable when the database changes; clients cannot set fields they shouldn't (e.g. `senderId`).
* **Without it:** mass-assignment bugs, leaked fields, schema changes break clients.
* **Mistakes:** returning entities "just this once"; one giant DTO for create + update + read.
* **Interview:** *"Why not return the entity?"* *"What is mass assignment?"*

## 7. Validation
* **Concept:** reject bad input at the boundary (fail fast).
* **In this project:** Bean Validation annotations on request DTOs (`@NotBlank`, `@Size`, `@Pattern`, `@Email`), `@Valid` in controllers and on STOMP payloads; Yup schemas mirror them on the frontend for instant feedback.
* **Why:** services can trust their input; users see field-level messages.
* **Without it:** 500 errors from nulls, 1 MB "messages", script injection attempts reaching the database.
* **Mistakes:** validating only on the frontend (bypassable with curl); validating in five places differently.
* **Interview:** *"Where do you validate and why both client and server?"* *"How do you return field errors?"*

## 8. MongoDB
* **Concept:** document database; schema designed from query patterns; embed vs reference.
* **In this project:** 10 collections, embedded `lastMessage`/reactions, referenced members/messages. See DATABASE-DESIGN.md.
* **Why:** fits chat's append-only, key-ordered access.
* **Without good modelling:** the original "messages inside room" design (16 MB limit, lost updates).
* **Mistakes:** modelling like SQL tables then doing N+1 lookups; unbounded arrays.
* **Interview:** *"Embed or reference — how do you decide?"* *"What's the 16 MB limit and how did it affect your design?"*

## 9. Repository pattern
* **Concept:** an interface that hides how data is stored.
* **In this project:** `MessageRepository extends MongoRepository<Message, String>` with derived queries (`findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc`).
* **Why:** services speak in domain terms; tests mock repositories.
* **Without it:** Mongo driver code inside services.
* **Mistakes:** derived method names so long nobody can read them (use `@Query` then); forgetting the index that each query needs.
* **Interview:** *"How does Spring Data generate a query from a method name?"* *"When would you use MongoTemplate?"*

## 10. Service / business logic
* **Concept:** the layer that enforces rules and orchestrates collaborators.
* **In this project:** `ChatServiceImpl.sendMessage` = membership → idempotency → rate limit → validation → persist → deliver → unread → notify.
* **Why:** one place for rules, shared by REST and WebSocket.
* **Without it:** duplicated rules in controllers, inconsistent behaviour between transports.
* **Mistakes:** god services; calling other controllers; transactions everywhere.
* **Interview:** *"Why a service layer if the controller could call the repository?"*

## 11. Exception handling
* **Concept:** throw meaningful exceptions, translate them in one place.
* **In this project:** `ChatAppException` + `ErrorCode`, `RestResponseExceptionHandler`, `@MessageExceptionHandler`, `SecurityErrorHandlers`, frontend `ApiError` + `handleError`. See ERROR-HANDLING.md.
* **Why:** consistent responses, no leaked stack traces, clean business code.
* **Without it:** try/catch in every method, inconsistent JSON, 500 for "not found".
* **Mistakes:** catching `Exception` and returning `null`; empty catch blocks; exposing `e.getMessage()` of internal errors.
* **Interview:** *"How does `@ControllerAdvice` work?"* *"Checked vs unchecked — which did you use and why?"*

## 12. Spring Security
* **Concept:** a chain of servlet filters that authenticates and authorises requests.
* **In this project:** `SecurityConfig` (stateless, CORS, public endpoints), `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`, custom entry point/denied handler.
* **Why:** a well-tested framework instead of homemade checks.
* **Without it:** every controller checking headers manually — one forgotten check = data leak.
* **Mistakes:** disabling CSRF without understanding why (we can: no cookies); `permitAll` on `/**`; registering a filter twice (we disable the auto servlet registration).
* **Interview:** *"Describe the filter chain."* *"Why is CSRF disabled in your app and when would that be wrong?"*

## 13. JWT
* **Concept:** signed token = header.payload.signature; the server verifies the signature instead of storing sessions.
* **In this project:** `JwtService` (HS256, `sub`, `username`, `jti`, `exp`), blacklist on logout, expired vs invalid distinguished.
* **Why:** stateless, works for WebSocket.
* **Without it:** session store + sticky sessions.
* **Mistakes:** putting secrets in the payload (it's only base64!); weak signing keys; no expiry; trusting `alg: none`.
* **Interview:** *"Is a JWT encrypted?"* *"How do you log out with JWT?"* *"Access vs refresh token?"*

## 14. WebSocket
* **Concept:** persistent full-duplex connection; STOMP adds pub/sub semantics.
* **In this project:** `WebSocketConfig`, `/ws`, `/topic/rooms/{id}`, `/user/queue/events`, heartbeats, auth interceptor. See WEBSOCKET-DESIGN.md.
* **Why:** server push for messages, typing, presence.
* **Without it:** polling — slow or wasteful.
* **Mistakes:** authenticating only the handshake (browsers can't send headers there); forgetting SUBSCRIBE authorization; assuming delivery = persistence.
* **Interview:** *"How do you authenticate a WebSocket?"* *"How would you scale WebSockets across servers?"*

## 15. Real-time messaging
* **Concept:** persist, then fan out; clients reconcile optimistic UI with server echoes.
* **In this project:** optimistic bubble with `clientMessageId` → server echo replaces it; unread pushed to private queues; read receipts via `lastReadAt`.
* **Why:** instant feel without lying to the user (pending / failed states exist).
* **Without it:** UI waits for round trips or shows messages that were never saved.
* **Mistakes:** matching echoes by content (two identical "ok" messages break it — we match by id).
* **Interview:** *"What happens if the receiver is offline?"* *"Optimistic UI — risks?"*

## 16. Concurrency
* **Concept:** multiple threads/requests touching shared state.
* **In this project:** unique indexes (DM rooms, memberships, idempotency), atomic Redis `INCR/HINCRBY`, connection counting for presence, `computeIfPresent` cleanup. See ARCHITECTURE §7 and `PresenceServiceConcurrencyTest`.
* **Why:** web servers handle requests in parallel; the same user has several tabs.
* **Without it:** duplicate rooms, wrong unread counts, users stuck "online".
* **Mistakes:** check-then-act in Java (`if (!exists) insert`) without a database constraint; `synchronized` in a multi-instance system.
* **Interview:** *"Two users click 'Message' at the same time — what happens?"* *"How are unread counts kept correct under concurrency?"*

## 17. Idempotency
* **Concept:** doing an operation twice has the same effect as once.
* **In this project:** `clientMessageId` + partial unique index; invite accept and "open DM" are naturally idempotent; logout twice is harmless.
* **Why:** retries are normal on mobile networks and in our WS→REST fallback.
* **Without it:** duplicated messages and double unread badges.
* **Mistakes:** dedupe by content; key generated on the server (a retry gets a new key); dedupe stored only in memory.
* **Interview:** *"How do you prevent duplicate messages?"* *"Is POST idempotent? How can you make it so?"*

## 18. Database indexes
* **Concept:** sorted data structures (B-trees) that let the DB find/sort without scanning.
* **In this project:** `{roomId:1, createdAt:-1}` for history, unique indexes for invariants, `userId` indexes for "my rooms/servers", `{userId, createdAt}` for notifications.
* **Why:** query time grows with the result size, not the collection size.
* **Without them:** history of one room scans every message of every room.
* **Mistakes:** wrong field order in compound indexes (equality → sort → range); indexing everything (slower writes); forgetting that `$or` needs an index per branch.
* **Interview:** *"Explain the ESR rule."* *"Why does `{roomId, userId}` not help `findByUserId`?"*

## 19. Pagination
* **Concept:** return data in slices.
* **In this project:** cursor `before=createdAt` for messages, offset for notifications/audit logs.
* **Why:** big histories; infinite scroll.
* **Without it:** loading 50 000 messages to show 30.
* **Mistakes:** offset pagination on live data (shifting pages); `count()` on every page.
* **Interview:** *"Offset vs cursor pagination?"* *"How do you know there's a next page without counting?"* (`Slice` fetches one extra.)

## 20. Testing
* **Concept:** automated proof that behaviour (including failures) is right.
* **In this project:** see TESTING.md — Mockito unit tests, MockMvc error contract, JWT, concurrency, real-Mongo integration.
* **Why:** refactor without fear; concurrency bugs found before users find them (two were).
* **Without it:** "works on my machine".
* **Mistakes:** only happy paths; asserting implementation details; flaky timing-based concurrency tests.
* **Interview:** *"Unit vs integration test?"* *"How do you test a race condition?"*

## 21. Logging
* **Concept:** structured, correlated, safe logs.
* **In this project:** SLF4J/Logback, `RequestIdFilter` puts a correlation id in the MDC, pattern `[req:%X{requestId}]`, access line per request, `requestId` in error responses, no tokens/passwords/bodies logged.
* **Why:** "it failed at 15:52" → `grep req:0912d672` → every line of that request.
* **Without it:** guessing which of 10 000 log lines belongs to the complaint.
* **Mistakes:** logging tokens or full request bodies; `e.printStackTrace()`; logging at ERROR for expected 404s.
* **Interview:** *"How would you debug a production issue reported by a user?"* *"What is MDC?"*

## 22. System design
* **Concept:** components, data flow, failure modes, trade-offs.
* **In this project:** ARCHITECTURE.md (flows, consistency, concurrency), DECISIONS.md.
* **Why:** explains *why* the code looks like it does.
* **Mistakes:** adding Kafka/microservices before there is a problem they solve.
* **Interview:** *"Design WhatsApp."* — use this project as your answer, then scale it (§9).

## 23. Scalability
* **Concept:** handle more load by adding resources.
* **In this project:** stateless JWT (any instance can serve REST), `RealtimeService` seam for a broker relay, shard-friendly `roomId` in every hot query, Redis for shared counters.
* **Without planning:** in-memory state that pins users to one server.
* **Mistakes:** scaling the app tier while the database is the bottleneck; sharding too early.
* **Interview:** *"What changes at 1 million users?"* (ARCHITECTURE §9.)

## 24. Deployment
* **Concept:** packaging, configuration, infrastructure.
* **In this project:** `docker-compose.yml`, executable jar, `next build`, environment variables, nginx WebSocket config. See DEPLOYMENT.md.
* **Mistakes:** baking secrets into images; forgetting `Upgrade` headers for WebSockets; `NEXT_PUBLIC_*` set after the build.
* **Interview:** *"How do you configure the app per environment?"* *"What does your proxy need for WebSockets?"*
