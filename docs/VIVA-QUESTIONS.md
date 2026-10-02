# Viva / Interview Questions & Answers

Short, confident answers you can say out loud, each pointing to the code that proves it.
Tip: answer in the shape **"What → Why → Where in my code → Trade-off"**.

---

## A. Project overview

**1. What is your project?**
Nexus, a real-time chat application that combines WhatsApp-style private chats and groups with Discord-style servers and channels. Backend: Spring Boot 3 + MongoDB + Redis + STOMP WebSockets with JWT security. Frontend: Next.js/React/TypeScript with Three.js visuals.

**2. What problem were you solving and what was hard?**
Real-time delivery that is also *reliable*: messages must not be lost when someone is offline, must not duplicate when the network retries, and unread/presence state must stay correct when many requests run in parallel.

**3. Explain the architecture in 30 seconds.**
The browser talks REST for request/response and STOMP over WebSocket for push. In Spring, a filter verifies the JWT, a thin controller validates the DTO, a service applies business rules and permission checks through `AccessGuard`, a repository saves to MongoDB, and `RealtimeService` pushes events to subscribers. Errors from any layer are translated by one global handler into a standard `Response` envelope.

**4. Why are there 10 collections?**
Each one has its own lifecycle and query pattern: users, servers, server_members, rooms, room_members, messages, invites, friendships, notifications, audit_logs. Membership is a separate collection because it holds per-member mutable state (role, `lastReadAt`) and is queried both "by room" and "by user".

## B. Database

**5. Why MongoDB?**
Chat data is document-shaped (a message with embedded reactions and mentions), mostly appended, and almost always read by `roomId` in time order, which maps perfectly to a compound index. The flexible schema let me add reactions, pins and mentions without migrations. The trade-off is no joins and limited transactions, so I designed invariants around single-document writes and unique indexes. (DECISIONS ADR-01)

**6. Why did you separate users, conversations and messages?**
The original code embedded all messages inside the room document. That hits the 16 MB document limit, rewrites the whole room on every send (lost updates under concurrency), and makes pagination awkward. Separate collections give O(1) inserts and an indexed history query. (DATABASE-DESIGN §2)

**7. How are the latest messages retrieved efficiently?**
`findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 30))` on the compound index `{roomId: 1, createdAt: -1}`. MongoDB jumps to that room in the index, walks the newest 30 entries and stops — no scan, no in-memory sort, same cost for a room with 10 or 10 million messages.

**8. Why are indexes required?**
Without an index, MongoDB scans every document in the collection, so a query gets slower as the *whole app* grows, not just that room. Indexes also enforce rules: unique indexes guarantee one DM per pair, one membership per user per room, and no duplicate message for the same `clientMessageId`.

**9. Which pagination did you use and why?**
Cursor pagination: `before=<createdAt of the oldest loaded message>`. Offset pagination (`skip`) gets slower with depth and shifts when new messages arrive, which causes duplicates or gaps while scrolling. `Slice` tells me `hasNext` without a `count()`.

**10. Embed or reference — how did you decide?**
Embed when it is small, bounded and always read together (reactions, mentions, the `lastMessage` preview). Reference when it grows unbounded or changes independently (messages, members).

**11. Do you use transactions?**
No. MongoDB multi-document transactions need a replica set and add latency. The important invariant (the message itself) is a single atomic insert. Derived data like the room preview and unread counters is eventually consistent and can be recomputed. Multi-step deletes go children-first, so a failure can simply be retried. (ARCHITECTURE §6)

## C. Real-time

**12. Why WebSocket?**
Chat needs server push with low latency in both directions. Polling is either slow or wasteful. I used STOMP on top because it gives topics, per-user queues and heartbeats, and Spring supports it natively.

**13. What happens if the receiver is offline?**
Nothing is lost. The message is saved in MongoDB first, and the receiver's unread counter is incremented in Redis. When they come back, the chat list shows the badge and opening the chat loads the history.

**14. What happens if the WebSocket disconnects?**
The client reconnects every 3 seconds and automatically re-subscribes to everything. While disconnected, sending falls back to REST. A message stuck "pending" can be retried with the same `clientMessageId`, so the retry can't create a duplicate.

**15. What happens if MongoDB fails?**
The save throws before any delivery, so nobody sees a message that was never stored. Spring translates the driver timeout into `DataAccessResourceFailureException`, and my handler returns `503 DB_503` (meaning "retry later", not a bug) with a request id. The sender's bubble shows "Not sent · retry". The test `sendMessage_databaseDown_exceptionPropagatesAndNothingDelivered` proves this.

**16. Are persistence and delivery the same operation?**
No, deliberately. Persistence must succeed or the request fails. Delivery is best effort, because offline users get the message from history. If delivery were tied to saving, messages would be lost for offline users.

**17. How do you prevent duplicate messages?**
The client generates a `clientMessageId` (a UUID) for each message and reuses it on every retry. MongoDB has a unique partial index on `{roomId, senderId, clientMessageId}`. A retry finds the stored message and returns it. If two copies race, the index lets exactly one insert win and the other catches `DuplicateKeyException` and returns the winner. An integration test with 10 concurrent threads proves only one document is stored.

**18. How do read receipts work?**
Each `room_members` row has a `lastReadAt`. Opening a chat sets it to now and broadcasts a `READ` event. My message shows blue ticks when every other member's `lastReadAt` is at or after the message time.

**19. How does online/offline presence work with multiple tabs?**
I keep a connection *count* per user in Redis (`HINCRBY`). The user goes online on the 0→1 transition and offline on 1→0, and `lastSeenAt` is saved at that point. Heartbeats detect dead sockets.

## D. Security

**20. How is authentication implemented?**
Registration hashes the password with BCrypt. Login verifies it and returns a JWT signed with HMAC-SHA256, containing the user id, username, a unique `jti` and an expiry. `JwtAuthenticationFilter` verifies the token on every request and puts an `AuthUser` in the security context.

**21. How does JWT work?**
It's `header.payload.signature`, Base64-encoded. The payload is readable by anyone, so it holds no secrets. The signature proves the server issued it and nobody changed it. The server doesn't store sessions; it just verifies the signature and expiry.

**22. How do you log out with a stateless token?**
On logout I put the token's `jti` in Redis with a TTL equal to its remaining lifetime. The filter rejects blacklisted ids with `AUTH_403`. The blacklist cleans itself up as tokens expire.

**23. How is authorization implemented?**
Two levels. Spring Security decides public vs authenticated endpoints. Resource-level rules live in `AccessGuard`: you must be a room member to read or post, an admin to create channels, the owner to change roles, and you can only kick roles lower than yours. The acting user always comes from the token, never from the request body.

**24. How is the WebSocket authenticated?**
Browsers can't add headers to the handshake, so the token goes in the STOMP `CONNECT` frame. `WebSocketAuthInterceptor` verifies it and sets the principal. It also checks every `SUBSCRIBE` to `/topic/rooms/{id}` against room membership, so nobody can eavesdrop on a room.

**25. Why is CSRF disabled?**
CSRF works by abusing cookies that the browser attaches automatically. My token is sent explicitly in a header, which a foreign site can't make the browser do. If I moved to cookie-based tokens, I would need CSRF protection again.

**26. How do you avoid leaking sensitive data?**
DTOs never contain `passwordHash`, and email appears only on your own profile. 500 and 503 responses have generic messages. Logs contain method, path, status and request id, but never tokens, passwords or bodies. Login gives the same error for unknown user and wrong password, so attackers can't find out which usernames exist.

## E. Code design

**27. Why DTOs?**
To separate the API contract from the database. They prevent leaking fields like the password hash and stop clients from setting fields they shouldn't, such as `senderId`. They also hold validation rules and let the database change without breaking clients.

**28. Why a service layer?**
Business rules must work the same for REST and WebSocket. `ChatService.sendMessage` serves both. Services are also where unit tests focus, because their dependencies can be mocked.

**29. How does exception handling work?**
Services throw subclasses of `ChatAppException` that carry an `ErrorCode`, and the code knows its HTTP status. `RestResponseExceptionHandler` turns any of them, plus validation, database and unexpected errors, into the same `Response` envelope. WebSocket errors use `@MessageExceptionHandler` and go to `/user/queue/errors`. On the frontend, everything becomes an `ApiError` handled by one `handleError` function.

**30. Why only ~6 exception classes?**
The status lives in the enum, so `ROOM_404` and `USER_404` don't need separate classes. The subclasses exist only where they make the throw site readable (`ForbiddenException`, `ResourceNotFoundException`, …). A new error is one enum line.

**31. Where did you apply SOLID?**
* **SRP:** `AccessGuard` (permissions), `MessageMapper` (mapping), `RealtimeService` (delivery), `RequestIdFilter` (correlation).
* **OCP:** new error codes need no handler changes.
* **LSP:** every `ChatAppException` subclass is handled through the base type.
* **ISP:** small interfaces like `RateLimitService` and `PresenceService`.
* **DIP:** services depend on `RealtimeService` and `UnreadService`, not on `SimpMessagingTemplate` or `StringRedisTemplate`.

**32. Where is DRY applied?**
`Response.ok/error`, `AccessGuard`, `RedisSafe`, `ReusableForm`, `handleError`, and the `request<T>()` unwrapper on the frontend.

**33. Where did you deliberately NOT abstract?**
Each controller explicitly wraps its result in `Response.ok(...)`. A magic "wrap every response" advice would hide what each endpoint returns. I also kept validation in both Yup and Bean Validation: the frontend gives instant feedback, the backend is the real guard. And I used no generic CRUD base service or base repository, because the entities behave too differently.

**34. Composition or inheritance?**
Composition: services are assembled from `AccessGuard`, mappers, `RedisSafe` and other services. The only inheritance is the exception hierarchy, because it's a genuine "is-a" and lets one handler catch them all.

**35. KISS and YAGNI examples?**
An in-memory STOMP broker instead of RabbitMQ. No Kafka, no microservices, no file uploads, no refresh tokens. Each has a documented path for when it's actually needed.

**36. Where do you use immutability?**
`AuthUser` is a record and is shared safely across threads. Enums are immutable. On the frontend, state is never mutated (`prev.map(...)`), which keeps React renders predictable.

## F. Concurrency and reliability

**37. How does the system handle concurrent requests?**
It pushes invariants down to atomic primitives: unique indexes for one DM per pair, one membership and one message per `clientMessageId`, and Redis `INCR`/`HINCRBY` for counters. No Java `synchronized` is involved, so it stays correct across multiple instances. (ARCHITECTURE §7)

**38. Two users open the same DM at the same moment?**
Both try to insert a room with the same `directKey`. The unique index accepts one, and the other catches `DuplicateKeyException` and loads the winner's room.

**39. Unread counts under concurrency?**
Redis `HINCRBY unread:{userId} roomId 1` is atomic, so there's no read-modify-write race in Java. If Redis is down, the count is computed from MongoDB as messages after `lastReadAt`.

**40. What happens if Redis is down?**
`RedisSafe` catches the failure, logs it once and skips Redis for 30 seconds. Presence switches to an in-memory map, unread counts come from MongoDB, and rate limiting fails open. The chat keeps working.

**41. Did testing find any real bugs?**
Yes, two.
1. The concurrency test showed that presence cleanup could erase a reconnect happening at the same moment. It's fixed with an atomic `computeIfPresent`.
2. The real-MongoDB idempotency test showed that 10 simultaneous retries of one message used up rate-limit quota, so some retries got 429. Now quota is counted only after a successful, non-duplicate insert.

**42. How would you debug a production issue?**
Every request gets a correlation id that appears in the `X-Request-Id` header, in every log line (`[req:…]`) and in error bodies. The UI even shows it for 5xx errors. The user quotes the reference, I grep it, and I see the whole request path with its timings.

## G. Scaling

**43. How would you scale the application?**
The REST tier is stateless (JWT), so I'd add instances behind a load balancer. For WebSockets I'd use sticky sessions and replace the in-memory broker with a broker relay (RabbitMQ) or Redis pub/sub; only `RealtimeServiceImpl` changes. MongoDB moves to a replica set, and Redis becomes required.

**44. What would change at 1 million users?**
* Shard `messages` on `roomId` (every hot query includes it).
* Split the WebSocket gateway from the REST API.
* Put an event stream (Kafka) between "message saved" and delivery, notifications and search.
* Compute unread counts for large channels on read instead of incrementing for every member.
* Archive old messages and add distributed tracing. (ARCHITECTURE §9)

**45. Why not microservices or Kafka now?**
They solve organisational and scale problems I don't have at 100–10,000 users, and they add network failure modes, deployment complexity and eventual consistency everywhere. The code has seams (`RealtimeService`, `UnreadService`) where they could plug in later.

## H. Trade-offs and reflection

**46. What trade-offs did you make?**
* JWT in `localStorage`: simple, but sensitive to XSS.
* A soft message rate limit, so retries never burn quota.
* Eventual consistency for previews and unread counts.
* Last-write-wins on emoji reactions.
* Cursor on `createdAt` only (rare boundary ties).
* An in-memory broker, so only one instance for now.

**47. What would you change for production?**
* Refresh tokens in HttpOnly cookies.
* Atomic `$addToSet`/`$pull` for reactions.
* A broker relay for multi-instance WebSockets.
* Actuator metrics and tracing.
* A MongoDB replica set.
* STOMP end-to-end tests and frontend tests.
* A composite `(createdAt, _id)` cursor.
* Delivered receipts and file attachments via object storage.

**48. What did you learn?**
Designing from query patterns, making invariants database-enforced instead of trusting application code, keeping persistence and delivery separate, and that tests (especially concurrency tests) find bugs that reading the code doesn't.

**49. How is your frontend's 3D not a performance problem?**
The Three.js scenes appear only on the landing, auth and invite pages. They're lazy-loaded on the client only, and wrapped in an error boundary that falls back to a CSS gradient if WebGL fails. The chat screens use plain CSS, so typing and scrolling stay smooth.

**49b. How is it deployed, and why does the first load take a minute?**
* The **frontend** is on Vercel (built once, served from a CDN over HTTPS).
* The **backend** is a Docker container on Render's free tier: a multi-stage Dockerfile builds the jar with Maven, then runs it on a small JRE image as a non-root user, with JVM memory capped for 512 MB.
* The **database** is MongoDB Atlas M0. Redis is optional (Upstash), because `RedisSafe` falls back when it's absent.

Free instances sleep after 15 idle minutes, so the first request after that is a ~1 minute cold start. The frontend pings `/api/v1/health` as soon as any page opens to start waking the backend early, and open WebSocket connections keep it awake. Configuration comes only from environment variables (`MONGODB_URI`, `JWT_SECRET`, `CORS_ORIGINS`), so no secrets are in git.

**49c. How did you make it work on phones?**
* Under 768px the Discord-style server rail turns into a bottom tab bar, and it hides inside an open chat (like WhatsApp).
* Sidebars and chats show one at a time with a back arrow.
* Message actions open on tap, because touch screens have no hover.
* Inputs use 16px text so iOS doesn't zoom in.
* The 3D scene uses fewer particles and a lower pixel ratio on small screens.
* The layout uses `100dvh` and safe-area padding so it fits phones with notches.

**50. Demo script (2 minutes)**
1. Landing page (3D).
2. Log in as `razi` in one browser and `aisha` in a private window.
3. Send a DM: it appears instantly, then the ticks turn blue when Aisha opens it.
4. Show the typing indicator, a reaction, a reply, an @mention notification.
5. Open Nexus HQ: channels, the member list with roles, invite link, audit log.
6. Stop Redis (`docker stop nexus-redis`): the chat still works.
7. Show Swagger and an error response with `errorCode` and `requestId`.
8. Run `./mvnw test`: 28 green tests.
