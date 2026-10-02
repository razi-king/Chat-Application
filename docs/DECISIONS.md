# Architectural Decision Records

Each record: **Decision · Why · Alternatives · Trade-offs · Consequences.**

---

### ADR-01 — MongoDB as the database
* **Decision:** MongoDB (Spring Data MongoDB).
* **Why:** chat data is document-shaped (a message with embedded reactions and mentions), append-heavy, read by one key (`roomId`) in time order — a perfect fit for a compound index. Flexible schema let features (reactions, pins, mentions) be added without migrations. It was also the stack the project started with.
* **Alternatives:** PostgreSQL (strong relations, joins, transactions — excellent too); Cassandra (massive write scale, but poor ad-hoc queries).
* **Trade-offs:** no joins → we batch-load related documents in mappers; uniqueness and consistency must be designed with indexes; multi-document transactions need a replica set.
* **Consequences:** query-driven schema, explicit indexes on every hot path, eventual consistency for derived data.

### ADR-02 — Separate `users`, `rooms`, `room_members`, `messages`
* **Decision:** reference instead of embedding for members and messages.
* **Why:** unbounded growth (16 MB limit), per-member mutable state (`lastReadAt`), and concurrency — embedding forced every send to rewrite one hot document.
* **Alternatives:** messages embedded in room (original design); bucketing (documents of 100 messages each).
* **Trade-offs:** more documents and an extra lookup for membership; bucketing would reduce document count but complicates edits/reactions.
* **Consequences:** the "latest 30 messages" query is a single index walk; membership checks are O(1) on a unique index.

### ADR-03 — One `Room` type for DMs, groups and channels
* **Decision:** `type = DIRECT | GROUP | CHANNEL` on one collection.
* **Why:** messages, read pointers, unread counts, pins, search and WebSocket topics work identically for all three — one code path instead of three.
* **Alternatives:** separate `conversations` and `channels` collections.
* **Trade-offs:** some fields are only meaningful for one type (`directKey` for DMs, `serverId` for channels) → sparse index, nullable fields.
* **Consequences:** WhatsApp and Discord features share `ChatPage` on the frontend and `ChatService` on the backend.

### ADR-04 — WebSocket with STOMP for real-time
* **Decision:** Spring's STOMP broker; REST stays for everything request/response.
* **Why:** server push with low latency; STOMP gives topics, per-user queues, heartbeats and auth hooks for free.
* **Alternatives:** polling, SSE, raw WebSocket with a custom protocol, Socket.IO (not native to Spring).
* **Trade-offs:** stateful connections complicate horizontal scaling (needs broker relay / sticky sessions).
* **Consequences:** `RealtimeService` abstracts delivery so the broker can be swapped; sending also works over REST as a fallback.

### ADR-05 — Redis, but optional at runtime
* **Decision:** Redis for presence counts, unread counters, rate limiting and the logout blacklist, wrapped by `RedisSafe` with fallbacks.
* **Why (problem → solution):** these are **hot, tiny, frequently-changing counters** shared by all requests. Redis gives atomic `INCR/HINCRBY` and TTLs, so concurrent updates need no locks and expired data cleans itself. Doing it in MongoDB means a write on every message per recipient and count queries for every chat list.
* **Alternatives:** MongoDB `$inc` counters (works, more DB load); in-memory maps (fast, but lost on restart and not shared between instances); Caffeine cache.
* **Honest note:** at 100 users MongoDB alone would be enough — Redis was a project requirement. That is why it is **optional**: if Redis is down, presence is kept in memory, unread counts are computed from `room_members.lastReadAt`, rate limiting fails open. Redis becomes *necessary* at the multi-instance stage (ARCHITECTURE §9).
* **Trade-offs:** one more service to run; two sources of truth for unread (Redis cache, MongoDB fallback) that can briefly disagree.
* **Consequences:** `RedisSafe` skips Redis for 30 s after a failure so a dead Redis costs one timeout, not one per request.

### ADR-06 — JWT (stateless) instead of server sessions
* **Decision:** HS256 JWT, 72 h lifetime, `jti` blacklist on logout.
* **Why:** no session store; same credential for REST and STOMP CONNECT; easy horizontal scaling.
* **Alternatives:** HTTP session + cookie (simple, but needs sticky sessions/shared store and CSRF protection); OAuth2 provider (overkill for a college app).
* **Trade-offs:** revocation needs the blacklist; token in `localStorage` is XSS-sensitive (see SECURITY.md).
* **Consequences:** `JwtAuthenticationFilter` + `WebSocketAuthInterceptor` are the only two places that read tokens.

### ADR-07 — Layered architecture (Controller → Service → Repository)
* **Decision:** classic layers with service interfaces, plus focused helpers (`AccessGuard`, mappers, `RealtimeService`).
* **Why:** matches the original project, easy to explain, clear dependency direction; business rules testable without HTTP or MongoDB.
* **Alternatives:** hexagonal / clean architecture with use-case classes and ports for every repository; feature packages (`chat/`, `server/`).
* **Trade-offs:** service classes can grow (ServerServiceImpl is the largest); interfaces with one implementation add files.
* **Consequences:** if a service grows further, split by use case (e.g. `ServerMembershipService`) — the interfaces make that a local change.

### ADR-08 — Exception strategy: one base exception + `ErrorCode` enum
* **Decision:** `ChatAppException(errorCode, message, meta)` with six semantic subclasses; status inside `ErrorCode`; one global handler per transport.
* **Why:** consistent envelope everywhere, new error = one enum constant, no try/catch in business code.
* **Alternatives:** one exception class per error (dozens of classes); returning error objects instead of throwing; Spring's `ProblemDetail` (RFC 7807).
* **Trade-offs:** our envelope is custom rather than RFC 7807 — chosen because the project already had `Response` and the frontend relies on it.
* **Consequences:** the frontend mirrors the codes in `ErrorCodes` and needs no per-endpoint error logic.

### ADR-09 — Cursor pagination for message history
* **Decision:** `?before=<createdAt>&size=30`, newest first, `Slice` for `hasNext`.
* **Why:** constant cost per page, no duplicates/gaps when new messages arrive while scrolling (offset pagination shifts).
* **Alternatives:** `page/size` offsets (original `message2` endpoint); cursor on ObjectId.
* **Trade-offs:** cannot jump to "page 50"; identical timestamps at a boundary could skip a message (fix: compound cursor).
* **Consequences:** infinite scroll upward in the UI; the same index serves page 1 and page 1000.

### ADR-10 — Idempotency via client-generated key + unique index
* **Decision:** the client generates `clientMessageId` (UUID) per message and reuses it on every retry; `{roomId, senderId, clientMessageId}` is unique (partial index).
* **Why:** networks retry; WebSocket send + REST fallback can both arrive. The database, not application locks, guarantees "once".
* **Alternatives:** server-side dedupe by content+time window (false positives: "ok" sent twice on purpose); Redis `SETNX` keys (extra moving part, TTL tuning); distributed locks.
* **Trade-offs:** clients must send the key (it is optional, so old clients still work without the guarantee).
* **Consequences:** a duplicate returns the original message; message rate limiting counts only successful inserts (soft limit).

### ADR-11 — Indexes chosen from query patterns
* **Decision:** every repository method has a matching index (see DATABASE-DESIGN.md); compound indexes ordered *equality fields first, sort field last*.
* **Why:** without them, the chat list, history and permission checks become collection scans that grow with total data, not with the user's data.
* **Trade-offs:** each index costs memory and slows writes slightly — so collections like `servers` stay unindexed beyond `_id` until needed.
* **Consequences:** an index audit found and fixed missing `userId` indexes on membership collections.

### ADR-12 — Persist first, deliver second
* **Decision:** save to MongoDB, then push over WebSocket; delivery failures never roll back the save.
* **Why:** the database is the source of truth; offline users get messages from history.
* **Consequences:** clients must reload history after reconnecting (they do), and duplicates are prevented by ADR-10, not by delivery logic.

### ADR-13 — No MongoDB transactions
* **Decision:** design invariants as single-document writes / unique indexes; derived data is eventually consistent.
* **Why:** transactions need a replica set, add latency on the hottest path, and the derived data (previews, counters) is recomputable.
* **Consequences:** multi-step deletes run children-first so a failure is retryable (ARCHITECTURE §6).

### ADR-14 — 3D only where it does not hurt usability
* **Decision:** React Three Fiber scenes on landing, auth and invite pages, lazy-loaded client-side with an error boundary; chat screens use CSS effects only.
* **Why:** the "futuristic" look without slowing typing/scrolling or breaking on machines without WebGL.
* **Alternatives:** 3D background behind the chat (rejected: GPU cost and distraction during long sessions).

### ADR-15 — Keep the original package structure (including `controler`)
* **Decision:** evolve, don't rename.
* **Why:** the brief was "don't change my basic structure"; renaming a package is cosmetic and makes the history harder to follow.
* **Trade-off:** the typo stays. It is a one-command refactor later.
