# Architecture

This document explains **what** each part of Nexus does, **why** it exists, and **how a request travels** through it.
Class names are real — open them while reading.

---

## 1. Starting point and what changed

**Assessment of the original project (≈40–50 % backend, basic frontend):**

| Problem found | Root cause | Fix |
|---|---|---|
| Messages were never broadcast | `@MessageMapping` returned a `ResponseEntity` and had no `@SendTo` | `RealtimeService` pushes to `/topic/rooms/{id}` after the message is saved |
| First message in a new room crashed (NPE) | `createRoom` condition was inverted, so the message list stayed `null` | Messages moved to their own collection; no embedded list to initialise |
| All messages embedded in the `Room` document | Every send loaded/saved the whole room; 16 MB document limit | `messages` collection + `{roomId, createdAt}` index |
| Both custom errors returned 400 | Handler hard-coded the status | `ErrorCode` carries its own `HttpStatus` |
| No auth; `sender` was a free-text string | No security layer | Spring Security + JWT; sender comes from the token |
| REST blocked by CORS | CORS only configured for WebSocket | Central `CorsConfigurationSource` |
| Entities returned to clients | No DTO boundary | Request/response DTOs + mappers |
| Frontend did not compile; env var unreadable; errors swallowed | `onSubmit={}`, missing `NEXT_PUBLIC_`, interceptor never rejected | Rewritten services, `ApiError`, rejected promises |

**Structure kept on purpose:** package names (`controler`, `service` + `service/impl`, `dto`, `entity`, `enums`, `exception`, `payload`, `repository`),
the `Response` envelope, the `ErrorCode` enum, `RoomControler`/`ChatController`, and on the frontend `ReusableForm`, `InputField`, the style enums,
`component/chat/*`, `services/*`. They were **evolved**, not replaced.

**Development phases followed:** (1) make it run (bugs, CORS, broadcast) → (2) DTOs + exception strategy → (3) users + JWT →
(4) messages collection + pagination → (5) Redis-backed presence/unread/rate-limit → (6) DMs, groups, servers, channels, roles →
(7) reactions, replies, pins, search, notifications, audit log → (8) idempotency, observability, DB-failure handling → (9) tests → (10) docs.

---

## 2. The big picture

```
                        ┌────────────────────────── Browser ──────────────────────────┐
                        │ Next.js pages → services/*.ts (axios) → REST                │
                        │ SocketContext (@stomp/stompjs)        → WebSocket /ws       │
                        └──────────────────────────────┬──────────────────────────────┘
                                                       │
 HTTP request                                          │ STOMP frame
   │                                                   │
   ▼                                                   ▼
 RequestIdFilter (correlation id, access log)     WebSocketAuthInterceptor
   ▼                                                (CONNECT = verify JWT, SUBSCRIBE = membership check)
 JwtAuthenticationFilter (token → AuthUser)            ▼
   ▼                                              ChatController @MessageMapping
 Spring Security (permitAll vs authenticated)          │
   ▼                                                   │
 Controller (@Valid DTO)  ─────────────────────────────┤
   ▼                                                   ▼
 Service interface  ─►  ServiceImpl  ─►  AccessGuard (permissions)
                            │      ├──►  Repository  ─►  MongoDB
                            │      ├──►  RealtimeService ─► SimpMessagingTemplate ─► subscribers
                            │      └──►  Presence / Unread / RateLimit / TokenBlacklist ─► RedisSafe ─► Redis
                            ▼
                     Mapper (entity → DTO)
   ▲
 Exceptions anywhere ─► RestResponseExceptionHandler / @MessageExceptionHandler ─► Response envelope
```

---

## 3. Layer by layer

Each component is described with the nine questions from the learning brief.

### 3.1 Controllers (`controler/`)
1. **What:** `AuthController`, `UserController`, `RoomControler`, `ChatController`, `ServerController`, `SocialController`.
2. **Why:** translate HTTP/STOMP into method calls — nothing else.
3. **Problem solved:** keeps HTTP details (paths, status codes, headers) out of business logic, so the same `ChatService.sendMessage` serves both REST and WebSocket.
4. **Called by:** Spring MVC / Spring messaging after security passed.
5. **Calls:** exactly one service method, then wraps the result with `Response.ok(data, message)`.
6. **Data:** request DTO in (validated by `@Valid`), response DTO out. The current user arrives as `@AuthenticationPrincipal AuthUser` (REST) or `Principal` (STOMP) — **never** from the request body.
7. **Can fail:** validation (400), anything thrown below — all handled centrally.
8. **Why this design:** thin controllers are trivially testable and cannot hide business rules.
9. **Alternative:** putting logic in controllers (what the original code started doing) — rejected because WebSocket and REST would duplicate it.

### 3.2 Services (`service/` interfaces, `service/impl/`)
1. **What:** `AuthService`, `UserService`, `RoomService`, `ChatService`, `ServerService`, `InviteService`, `FriendService`, `NotificationService`, `AuditLogService`, plus infrastructure-facing `PresenceService`, `UnreadService`, `RateLimitService`, `TokenBlacklistService`, `RealtimeService`.
2. **Why:** this is where the **rules** of the product live: "only members can post", "owner cannot leave", "a retry returns the same message".
3. **Problem solved:** one place to read, test and change behaviour.
4. **Called by:** controllers, the WebSocket controller, the demo seeder, other services.
5. **Calls:** repositories, `AccessGuard`, mappers, `RealtimeService`, Redis-backed services.
6. **Data:** IDs + DTOs in, DTOs out. Entities never leave this layer.
7. **Can fail:** throws `ChatAppException` subclasses for rule violations; database exceptions propagate to the global handler.
8. **Why interfaces?** Controllers depend on `ChatService`, not `ChatServiceImpl` (Dependency Inversion). In tests `ChatServiceImpl` gets mocks of its collaborators. Honestly: with one implementation each, interfaces are mostly a learning/structure choice kept from the original project; the ones that truly pay off are the infrastructure ports (`RealtimeService`, `PresenceService`, `RateLimitService` …) whose implementation could be swapped (e.g. a Redis broker relay) without touching business code.
9. **Alternative:** "use case" classes per operation (Clean Architecture). Rejected as too many tiny classes for this size (KISS).

### 3.3 `AccessGuard` (`security/AccessGuard.java`)
Central permission checks: `requireRoomMember`, `requireServerRole(serverId, userId, MemberRole.ADMIN)`, `canModerate(room, userId)`.
**Why:** authorization rules were about to be copy-pasted into every service method. One class = one place to audit "who may do what" (least privilege).
**Fails with:** `ForbiddenException(NOT_ROOM_MEMBER / NOT_SERVER_MEMBER / ACCESS_DENIED)`, `ResourceNotFoundException`.

### 3.4 Repositories (`repository/`)
Spring Data interfaces; method names become MongoDB queries (`findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc`).
**Why:** no hand-written query code for simple lookups; every query maps to an index documented in DATABASE-DESIGN.md.
**Alternative:** `MongoTemplate` everywhere — more control, more code. Used only where needed (tests).

### 3.5 Mappers (`mapper/`)
`UserMapper`, `MessageMapper`, `RoomMapper` convert entities into response DTOs.
**Why separate:** mapping a page of 30 messages needs the 30 senders and reply targets. `MessageMapper.toResponses` loads them in **one query each** (`findAllById`) instead of 60 queries (the N+1 problem). Keeping this in services would bloat them.

### 3.6 Real-time (`RealtimeService`, `WebSocketConfig`, `WebSocketAuthInterceptor`, `WebSocketEventListener`)
Persistence and delivery are **separate steps**: services save first, then call `RealtimeService.toRoom/toUser`. See WEBSOCKET-DESIGN.md.

### 3.7 Security (`security/`, `config/SecurityConfig`)
`JwtService` (sign/verify), `JwtAuthenticationFilter` (HTTP), `WebSocketAuthInterceptor` (STOMP), `SecurityErrorHandlers` (401/403 in the standard envelope). See SECURITY.md.

### 3.8 Exceptions (`exception/`, `enums/ErrorCode`)
Business code **throws**, it never builds error responses. `RestResponseExceptionHandler` (REST) and `ChatController`'s `@MessageExceptionHandler`s (STOMP) translate. See ERROR-HANDLING.md.

### 3.9 Redis-backed services + `RedisSafe`
Presence, unread counters, rate limiting, logout blacklist. Every call goes through `RedisSafe.call(redisCall, fallback)`; when Redis is down the app degrades (in-memory presence, unread counted from MongoDB, rate limit fails open) instead of failing. See DECISIONS.md ADR-05.

### 3.10 Observability (`config/RequestIdFilter`)
Each HTTP request gets a correlation id in the MDC, so every log line shows `[req:9f1c…]`, the response carries `X-Request-Id`, and error bodies include `requestId`. A user can quote it and you `grep` the exact log lines.

---

## 4. Request flows

### 4.1 Register / Login
```
Client POST /api/v1/auth/login {identifier, password}
 → RequestIdFilter (id = 9f1c…)
 → JwtAuthenticationFilter (no token, continue)
 → SecurityConfig: /auth/login is permitAll
 → AuthController.login  (@Valid LoginRequest: both fields @NotBlank → else 400 VAL_400)
 → AuthServiceImpl.login
     1. normalise identifier (trim + lowercase)
     2. RateLimitService.check(identifier, "login", 10, 60s)     → 429 RATE_429 when brute-forced
     3. findByEmail / findByUsername                              → not found  → 401 AUTH_402
     4. passwordEncoder.matches(raw, bcryptHash)                  → mismatch   → 401 AUTH_402 (same error: no user enumeration)
     5. JwtService.generateToken(user)  (sub=userId, username, jti, exp=72h, HS256)
 → Response.ok(AuthResponse{token, expiresAt, user}) → 200
Client stores the token, sends it as "Authorization: Bearer …" on every request and in the STOMP CONNECT frame.
```

### 4.2 Authenticated request
```
GET /api/v1/rooms  Authorization: Bearer xyz
 → JwtAuthenticationFilter: parse + verify signature + expiry → check jti not blacklisted (Redis)
     ok      → SecurityContext = AuthUser(id, username)
     invalid → request attribute auth.errorCode = AUTH_401 / AUTH_403, continue unauthenticated
 → Spring Security: endpoint requires authentication → SecurityErrorHandlers.commence → 401 + envelope
 → RoomControler.myConversations(@AuthenticationPrincipal AuthUser me) → RoomService …
```

### 4.3 Sending a message (the core flow)
```
Sender (Razi)                     Backend                                              Receiver (Aisha)
──────────────                    ───────                                              ────────────────
optimistic bubble (⏱)
STOMP SEND /app/rooms/R/send
 {content, replyToId, clientMessageId=c-uuid}
                       → WebSocketAuthInterceptor: socket already authenticated at CONNECT
                       → ChatController.sendMessage(@Valid payload, principal)
                       → ChatServiceImpl.sendMessage
                           1. AccessGuard.requireRoom / requireRoomMember        (404 / 403)
                           2. idempotency lookup {roomId, senderId, clientMessageId}
                                 found → re-broadcast stored message, return       (no duplicate)
                           3. rateLimit.ensureBelowLimit (8 per 5 s)              (429)
                           4. validate replyTo belongs to the same room          (400)
                           5. resolve @mentions to room members
                           6. INSERT message  ← unique index rejects a racing duplicate
                                 DuplicateKeyException → return the winner
                           7. rateLimit.record
                           8. update room.lastMessage, sender lastReadAt          (best effort, see §6)
                           9. RealtimeService.toRoom  → /topic/rooms/R            ──────────►  bubble appears
                          10. for each other member: UnreadService.increment (Redis HINCRBY)
                              → /user/queue/events UNREAD                         ──────────►  badge +1
                          11. mention notifications → notifications collection + /user/queue/events
bubble swaps temp → saved (matched by clientMessageId), shows ✓
                                                                      Aisha opens the chat:
                                                                      POST /rooms/R/read → lastReadAt=now,
                                                                      Redis HDEL unread, broadcast READ
✓✓ turns blue  ◄───────────────────────────────────────────────────────────────────── READ event
```
If the WebSocket is down, the client sends the same payload with `POST /api/v1/rooms/R/messages`; if the echo never arrives within 8 s the bubble shows "Not sent · retry", and the retry reuses the **same** `clientMessageId`.

### 4.4 Error flow
```
Invalid body → MethodArgumentNotValidException → RestResponseExceptionHandler.handleValidation
 → 400 {success:false, errorCode:"VAL_400", errorMeta:{content:"Message cannot be empty"}, requestId}
 → axios interceptor → ApiError → handleError() → Formik field error + toast
```
All other error flows: ERROR-HANDLING.md.

---

## 5. Engineering principles — where they actually appear

| Principle | Where | The problem it solves here |
|---|---|---|
| **SRP** | `AccessGuard` (permissions only), `MessageMapper` (mapping only), `RealtimeService` (delivery only), `RequestIdFilter` | `ChatServiceImpl` stays about chat rules; permission logic can be audited in one file |
| **OCP** | `ErrorCode` enum + `ChatAppException` | A new error = one enum constant; the handler never changes |
| **LSP** | `ResourceNotFoundException`, `ForbiddenException` … extend `ChatAppException` | The handler treats any subclass correctly through the base type |
| **ISP** | `RateLimitService` (3 methods), `PresenceService`, `UnreadService` are small, separate interfaces | `AuthServiceImpl` depends only on rate limiting, not on presence |
| **DIP** | Services depend on `RealtimeService`, not `SimpMessagingTemplate`; on `UnreadService`, not `StringRedisTemplate` | Business logic is unaware of STOMP/Redis; tests replace them with mocks; a broker relay could replace the simple broker |
| **DRY** | `Response.ok/error`, `AccessGuard`, `RedisSafe`, `ReusableForm`, `handleError`, `request<T>()` unwrapper | Error envelope / permission / fallback code exists once |
| **Deliberate duplication** | Each controller writes `ResponseEntity.ok(Response.ok(...))`; DTO validation annotations are repeated on Yup schemas | A generic "auto-wrap all responses" advice would hide what each endpoint returns; frontend validation gives instant feedback, backend validation is the real guard — both are needed |
| **KISS** | Simple in-memory STOMP broker; no event bus; plain Spring Data method names | Enough for one instance; documented upgrade path |
| **YAGNI** | No file uploads, no microservices, no Kafka, no refresh tokens | Not required for the brief; extension points exist (see §9) |
| **Composition over inheritance** | Services compose `AccessGuard`, mappers, `RedisSafe`. Only the exception hierarchy uses inheritance (it is a true "is-a" and lets one handler catch them all) | Behaviour is assembled, not inherited — no fragile base service class |
| **Fail-fast** | `@Valid` on every DTO, `requireX` guards at the top of each method, `JwtService` throws on bad tokens | Invalid work stops before touching the database |
| **Immutability** | `AuthUser` is a `record`; `ErrorCode` is immutable; the frontend never mutates state (always `prev.map(...)`) | Thread-safe sharing of the principal across threads; predictable React renders |
| **Least privilege** | `AccessGuard`, role ranks (`MemberRole.atLeast`), sender always from the token, SUBSCRIBE check | A user cannot read/post in rooms they are not in or kick people above them |
| **Observability** | `RequestIdFilter`, log pattern `[req:…]`, `requestId` in error bodies | Trace one user's failed request in the logs |

---

## 6. Consistency and transactions

MongoDB multi-document transactions need a replica set; the dev setup is a single node, and more importantly **most operations do not need them**:

| Operation | Must be atomic | Can be eventual | If step N fails |
|---|---|---|---|
| Send message | The message insert (single document = atomic) | `room.lastMessage`, sender `lastReadAt`, Redis unread, notifications | Message exists (source of truth). A stale preview fixes itself on the next message; unread counts can be recomputed from `lastReadAt` (that is exactly the Redis fallback). |
| Idempotent send | Uniqueness of `{roomId, senderId, clientMessageId}` | — | Enforced by a unique index, not by application locks |
| Create server | — | Channels, owner membership, audit log | A crash leaves a server with fewer channels; the owner can still add channels. Order: server → owner member → channels, so the owner is never locked out |
| Delete server | — | Children deleted **first**, server document **last** | A partial failure leaves the server visible → the owner retries delete. Deleting the parent first would orphan children invisibly |
| Join via invite | Membership uniqueness (`{serverId,userId}` unique index) | `uses` counter | Double click cannot create two memberships |
| Open DM | One room per pair (`directKey` unique index) | — | Two users clicking "Message" together: the loser catches `DuplicateKeyException` and loads the winner's room |

**Rule used:** make the *important invariant* a single-document write or a unique index; let derived data be eventually consistent and recomputable.

---

## 7. Concurrency

| Race | Where | How it is handled |
|---|---|---|
| Two users send at the same time | `ChatServiceImpl.sendMessage` | Independent inserts; ordering by `createdAt` (+ ObjectId). No shared mutable document is rewritten (the old design rewrote the whole room → lost updates) |
| Same message submitted twice (retry, WS + REST) | `messages.idempotency_key` unique index | Exactly one insert wins; others return it. Proven by `ChatAppBackendApplicationTests.concurrentRetriesOfSameMessage_storeExactlyOneCopy` (10 threads) |
| Unread counters updated concurrently | `UnreadServiceImpl` | Redis `HINCRBY` is atomic — no read-modify-write in Java |
| Multiple tabs / sockets per user | `PresenceServiceImpl` | Connection **count** per user (`HINCRBY`), online only when it goes 0→1, offline only at 1→0. Cleanup uses `computeIfPresent(v ≤ 0)` so a reconnect between decrement and cleanup is not lost. Proven by `PresenceServiceConcurrencyTest` (64 threads) |
| Rate limiting | `RateLimitServiceImpl` | Atomic `INCR` for login (strict). For messages: check, then count only after a successful non-duplicate insert (soft limit — retries never burn quota) |
| Reactions toggled concurrently | `ChatServiceImpl.react` | Read-modify-write of one message document — last write wins. Acceptable for emoji; the fix at scale is an atomic `$addToSet`/`$pull` update (documented trade-off) |
| Two people open the same DM | `RoomServiceImpl.openDirect` | `directKey` unique index + catch `DuplicateKeyException` |
| WebSocket reconnect storms | Client | `reconnectDelay: 3000`, server heartbeats every 10 s detect dead sockets |

---

## 8. Frontend architecture (short)

* **Contexts:** `AuthContext` (token + user, listens for `nexus:auth-expired`), `SocketContext` (one STOMP client, re-subscribes after reconnect, routes `/user/queue/errors` to `handleError`), `AppStateContext` (chat list, servers, presence map, unread), `ServerContext` (current server detail).
* **Services:** one class per backend area returning plain data (`request<T>` unwraps the envelope).
* **Errors:** every failure becomes `ApiError` (`lib/ApiError.ts`) → `handleError()` shows toast / field errors; `app/error.tsx` catches render crashes; the 3D canvas has its own error boundary so a WebGL failure never breaks a page.
* **3D:** only on landing, auth and invite pages, lazy-loaded with `ssr:false`. The chat screens use CSS only, so the chat stays fast.

---

## 9. Scaling from 100 to 1 million users

| Users | What breaks first | Change |
|---|---|---|
| **100** (college) | Nothing | Current design: one Spring Boot instance, one MongoDB, optional Redis |
| **10 000** | Simple broker memory, slow queries without indexes | Keep indexes (done), enable MongoDB replica set (HA + transactions possible), run Redis for real (not fallback), add `/actuator` health + metrics, CDN for the frontend |
| **100 000** | One JVM can hold ~tens of thousands of sockets; broker is in-process, so users on instance A cannot receive messages published on instance B | Several backend instances behind a load balancer with sticky WebSocket sessions; replace `enableSimpleBroker` with `enableStompBrokerRelay` (RabbitMQ) or Redis pub/sub fan-out — `RealtimeService` is the only class that changes. Move notifications/mention fan-out to an async queue. Rate limits per IP at the gateway |
| **1 000 000** | `messages` collection size and write rate; large servers (fan-out of unread increments to thousands of members) | Shard `messages` on `{roomId: hashed}` or `{roomId, createdAt}` (all hot queries include `roomId`); separate WebSocket gateway service from the REST API; Kafka (or similar) for message events → delivery workers, notification workers, search indexer; per-channel read pointers instead of per-member unread increments for big servers (count on read); TTL/archival of old messages to cold storage; distributed tracing (OpenTelemetry) using the existing correlation ids |

The college version intentionally stops at the first row; the seams (`RealtimeService`, `UnreadService`, `RedisSafe`, index design) are where the later rows plug in.
