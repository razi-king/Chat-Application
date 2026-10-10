# 10. Likely viva questions (with short answers)

How to answer: say **what** it is, **why** you chose it, **where** it is in the code, and one **trade-off**. Short, confident answers beat long ones.

### About the project
**Q1. What is your project?**
Nexus is a full-stack real-time chat application. It combines WhatsApp-style private chats and groups with Discord-style servers and channels. The backend is Spring Boot with MongoDB, Redis and WebSockets; the frontend is Next.js with a 3D landing page.

**Q2. What problem does it solve?**
Students use two apps: WhatsApp for private chats and Discord for communities. Nexus puts both in one app. It also solves engineering problems that simple chat apps ignore: messages lost when a user is offline, and duplicate messages when the network retries.

**Q3. What were your objectives?**
One app for DMs, groups, servers and channels; real-time delivery with read ticks, typing and presence; secure JWT login with role-based permissions; no lost or duplicate messages; and a documented, tested, deployed system.

**Q4. List the main features.**
WhatsApp side: 1-to-1 chats, groups, read receipts, typing indicator, last seen, reply, reactions, edit, delete, pin, unread badges. Discord side: servers, channels, Owner/Admin/Member roles, invite links, public server discovery, audit log. Also friends, notifications, @mentions and message search.

**Q5. What makes your project different from a normal chat project?**
Reliability and engineering depth: messages are saved before they are delivered, duplicate sends are blocked by a unique database index, Redis failures do not break the app, every error has a typed code, and there are 33 automated tests including concurrency tests.

**Q6. Who are the users and what roles exist?**
Any registered user. Inside a server there are three roles: Owner, Admin and Member. Inside a group the creator is Owner. Roles are per server, not global.

### Technology and architecture
**Q7. Explain your architecture in 30 seconds.**
The browser talks to the backend in two ways: REST over HTTPS for request/response (login, history) and a WebSocket for live push (messages, typing, ticks). In Spring Boot a filter verifies the JWT, a controller validates the DTO, a service applies business rules and permission checks, a repository saves to MongoDB, and a realtime service pushes events. One global handler converts every error into a standard JSON response.

**Q8. Why Spring Boot?**
It gives a production-ready web server, dependency injection, validation, security and WebSocket support out of the box. It also matched the original project, so I evolved it instead of rewriting.

**Q9. Why Next.js and React?**
Component-based UI, TypeScript for type safety, file-based routing, and a production build that deploys easily on Vercel.

**Q10. What are the layers of your backend?**
Controller (receives requests, validates DTOs), Service (business rules; AccessGuard checks permissions), Repository (Spring Data MongoDB queries), and the database. Controllers stay thin so the same service works for both REST and WebSocket.

**Q11. Why do you have a service layer?**
Business rules must behave the same for REST and WebSocket. `ChatService.sendMessage` serves both. Services are also easy to unit-test because their dependencies can be mocked.

**Q12. What are DTOs and why use them?**
Data Transfer Objects define exactly what goes in and out of the API. They prevent leaking fields like the password hash, stop clients from setting fields like senderId, carry validation rules, and let the database change without breaking clients.

**Q13. Why both REST and WebSocket?**
REST suits request/response actions and is easy to document (Swagger) and retry. WebSocket suits server push. Sending a message uses WebSocket first and falls back to REST if the socket is down.

**Q14. What is STOMP?**
A simple messaging protocol on top of WebSocket. It adds destinations (`/topic/rooms/{id}`), subscriptions and per-user queues (`/user/queue/events`), so I did not have to invent my own protocol. Spring supports it natively.

**Q15. Where did you apply SOLID?**
Single responsibility: AccessGuard only checks permissions, MessageMapper only maps. Open/closed: a new error is one ErrorCode constant. Liskov: every ChatAppException subclass is handled through the base type. Interface segregation: small interfaces like RateLimitService. Dependency inversion: services depend on the RealtimeService interface, not on the WebSocket template.

**Q16. What is dependency injection?**
Spring creates objects and passes their dependencies through the constructor (Lombok `@RequiredArgsConstructor` with final fields). Classes become testable, because tests pass mocks through the same constructor.

### MongoDB and the data model
**Q17. Why MongoDB?**
Chat data is document-shaped (a message with embedded reactions and mentions), mostly appended, and almost always read by one key (roomId) in time order, which fits a compound index perfectly. The flexible schema let me add features without migrations. Trade-off: no joins and limited transactions, so rules are enforced with single-document writes and unique indexes.

**Q18. How many collections and what are they?**
Ten: users, rooms, room_members, messages, servers, server_members, invites, friendships, notifications, audit_logs.

**Q19. Why did you separate users, rooms and messages?**
The original design stored all messages inside the room document. That hits MongoDB's 16 MB document limit, rewrites the whole room on every message (lost updates under concurrency) and makes pagination hard. A separate messages collection gives fast inserts and an indexed history query.

**Q20. Embedded or referenced, how did you decide?**
Embed when data is small, bounded and always read together: reactions, mentions, the lastMessage preview. Reference when it grows without limit or changes independently: messages and members.

**Q21. How are the latest messages loaded efficiently?**
With the compound index `{roomId: 1, createdAt: -1}`. MongoDB jumps to that room in the index, reads the newest 30 entries and stops: no full scan, no in-memory sort, same speed for a small or a huge database.

**Q22. Why are indexes required?**
Without an index MongoDB scans every document, so queries slow down as the whole app grows. Unique indexes also enforce rules: one DM room per pair, one membership per user per room, one message per clientMessageId.

**Q23. Which pagination did you use?**
Cursor pagination: `?before=<createdAt of the oldest loaded message>&size=30`. Offset pagination (skip) gets slower with depth and shifts when new messages arrive, causing duplicates or gaps while scrolling.

**Q24. Do you use MongoDB transactions?**
No. They need a replica set and add latency. The message insert is a single atomic write. Derived data like the chat preview and unread counts is eventually consistent and can be recomputed.

**Q25. What is the 16 MB limit?**
The maximum size of one MongoDB document. Unlimited messages in one room document would eventually break, which is why messages have their own collection.

**Q26. How do read receipts work in the database?**
Each room_members row has `lastReadAt`. Opening a chat sets it to now. A message shows blue ticks when every other member's lastReadAt is at or after the message time.

### Real-time messaging
**Q27. Why WebSocket instead of polling?**
Chat needs the server to push data instantly. Polling is either slow or wasteful. WebSocket keeps one open two-way connection, so messages, typing and ticks arrive immediately.

**Q28. Explain the flow when a user sends a message.**
1. The bubble appears instantly with a unique clientMessageId.
2. It goes over the WebSocket; the JWT was verified on connect.
3. The service checks: room member? duplicate? rate limit?
4. The message is saved in MongoDB.
5. It is pushed to everyone subscribed to `/topic/rooms/{id}`.
6. Redis increments other members' unread counts; @mentions create notifications.

**Q29. What happens if the receiver is offline?**
Nothing is lost. The message is saved in MongoDB first and the unread counter goes up. When the user returns, the chat list shows the badge and opening the chat loads the history.

**Q30. What happens if the WebSocket disconnects?**
The client reconnects every 3 seconds and re-subscribes automatically. While disconnected, sending falls back to REST. A pending message can be retried with the same clientMessageId, so it is never duplicated.

**Q31. Are saving and delivering the same operation?**
No, on purpose. Saving must succeed or the request fails. Delivery is best effort, because offline users get the message from history.

**Q32. How do you prevent duplicate messages?**
The client generates a clientMessageId (UUID) per message and reuses it on retry. MongoDB has a unique index on `{roomId, senderId, clientMessageId}`. A retry finds the stored message and returns it; if two copies race, the index lets exactly one win. A test with 10 parallel retries stores exactly one message.

**Q33. What happens if MongoDB fails while sending?**
The save throws before any delivery, so nobody sees a message that was never stored. The user gets 503 DB_503 with a request id, and the bubble shows "Not sent, retry".

**Q34. How does the typing indicator work?**
The composer sends a typing event at most every 2.5 seconds while typing. Nothing is saved. Receivers hide it after 5 seconds if no stop event arrives.

**Q35. How does online/offline presence handle multiple tabs?**
Redis keeps a connection count per user. Online when it goes from 0 to 1, offline only when it returns to 0; then lastSeenAt is saved in MongoDB.

**Q36. What is optimistic UI?**
Showing the message immediately, before the server confirms it, then replacing it with the saved version when the server echo arrives (matched by clientMessageId).

### Security and RBAC
**Q37. How are passwords stored?**
Never in plain text. They are hashed with BCrypt, which is salted and deliberately slow, so a leaked database does not reveal passwords.

**Q38. How does JWT authentication work?**
After login the server creates a token: header, payload (user id, username, unique id, 72 h expiry) and a signature made with a secret key. The client sends it as `Authorization: Bearer` on every request. A filter verifies signature and expiry; if valid, the user is set for that request. No sessions are stored.

**Q39. Is a JWT encrypted?**
No, only Base64-encoded and signed. Anyone can read the payload, so it contains no secrets. The signature proves the server issued it and nobody changed it.

**Q40. How do you log out with a stateless token?**
On logout the token's unique id (jti) is saved in Redis until the token would expire. The filter rejects blacklisted tokens with AUTH_403. The list cleans itself.

**Q41. Authentication vs authorization?**
Authentication: who you are (login, JWT). Authorization: what you may do (for example, only a server admin can create channels).

**Q42. Did you implement RBAC? Explain it.**
Yes, role-based access control per server. Roles Owner, Admin and Member are stored in server_members. Members chat, react and invite; Admins also delete others' messages, create channels and kick Members; only the Owner changes roles or deletes the server. You can only act on someone below your rank. AccessGuard enforces this in the backend and returns 403 otherwise.

**Q43. Why are roles in server_members and not in users?**
A role belongs to the relationship between a user and a server. Razi can be Owner of one server and Member of another.

**Q44. How is the WebSocket secured?**
Browsers cannot add headers to the WebSocket handshake, so the token is sent in the STOMP CONNECT frame and checked by an interceptor. Every SUBSCRIBE to a room topic is checked against membership.

**Q45. Why is CSRF protection disabled?**
CSRF abuses cookies that browsers send automatically. Nexus sends the token explicitly in a header, which another website cannot make the browser do. If tokens moved to cookies, CSRF protection would be needed again.

**Q46. How do you avoid leaking sensitive data?**
DTOs never include the password hash; email is shown only on your own profile; 500/503 errors return generic messages; logs never contain tokens or passwords; login gives the same error for an unknown user and a wrong password.

### Redis, error handling and concurrency
**Q47. Why did you use Redis?**
For small numbers that change on almost every action: presence, unread counts, rate limits and the logout blacklist. Redis is in memory, very fast, has atomic commands (INCR, HINCRBY) and keys that expire automatically.

**Q48. What happens if Redis goes down?**
RedisSafe catches the failure, logs it once and skips Redis for 30 seconds. Presence moves to memory, unread counts come from MongoDB, rate limiting lets requests through. The chat keeps working.

**Q49. Which Redis keys exist?**
`presence:connections`, `unread:<userId>`, `rate:<action>:<user>:<window>`, `auth:blacklist:<tokenId>`.

**Q50. How does your exception handling work?**
Services throw custom exceptions extending ChatAppException with an ErrorCode (for example ROOM_404) that knows its HTTP status. One global handler (`@ControllerAdvice`) turns every exception into the same JSON envelope with success, message, errorCode and requestId. WebSocket errors use the same envelope.

**Q51. Why only about six exception classes?**
The HTTP status lives in the ErrorCode enum, so ROOM_404 and USER_404 share one class. A new error is one enum line.

**Q52. What is the requestId?**
A correlation id given to every request. It is in the X-Request-Id header, every log line and error responses, so an error report can be matched to the exact server logs.

**Q53. How does the system handle concurrent requests?**
Rules are enforced by atomic database and Redis operations, not Java locks: unique indexes and atomic Redis counters. This also works across multiple servers.

**Q54. Two users open the same DM at the same time?**
Both insert a room with the same directKey. The unique index accepts one; the other catches DuplicateKeyException and loads the winner's room.

**Q55. What is idempotency?**
Doing an operation twice has the same effect as once. Re-sending a message with the same clientMessageId, accepting an invite twice or opening the same DM twice never creates duplicates.

**Q56. What is rate limiting?**
Limiting how often a user can act, to stop spam and brute force: 8 messages per 5 seconds and 10 logins per minute, counted in Redis. Excess requests get 429 RATE_429. On top of that, 5 wrong passwords in a row lock the account for 15 minutes (429 AUTH_429), even if the attacker then tries the correct password; a successful login resets the counter. If Redis is down, the counters run in the server's memory instead of switching off.

### Testing, deployment and scaling
**Q57. How did you test the project?**
33 automated tests: Mockito unit tests (register, login, messages, permissions), MockMvc tests for every error status, JWT tests, a concurrency test with 64 parallel connections, and an integration test against real MongoDB.

**Q58. Did testing find real bugs?**
Yes, two race conditions: a reconnect could be erased during presence cleanup (fixed with atomic computeIfPresent), and 10 parallel retries used up the rate limit (now counted only after a successful, non-duplicate save).

**Q59. Unit test vs integration test?**
A unit test checks one class with mocked dependencies and is fast. An integration test runs real components together, here a real MongoDB, to prove things mocks cannot.

**Q60. How is the project deployed?**
Frontend on Vercel, backend as a Docker container on Render's free tier (Singapore), database on MongoDB Atlas, Redis on Upstash. All secrets are environment variables, never in GitHub.

**Q61. Why Docker?**
It packages Java and the app the same way everywhere. Multi-stage: Maven builds the jar, a small Java runtime runs it as a non-root user with memory limits for 512 MB. It uses about 162 MB.

**Q62. Why does the first load take about a minute?**
Render's free tier sleeps after 15 minutes without traffic. The website pings the health endpoint as soon as a page opens, so the server starts waking early.

**Q63. How would you scale to 1 million users?**
Several backend instances behind a load balancer (JWT is stateless), a shared WebSocket broker (RabbitMQ or Redis pub/sub), a MongoDB replica set with messages sharded by roomId, a message queue (Kafka) for notifications and search, and monitoring with tracing.

**Q64. Why not microservices or Kafka now?**
They solve problems of very large teams and traffic that a college-scale app does not have, and add complexity and failure points. The code has seams (RealtimeService, UnreadService) where they can be added later.

**Q65. What trade-offs did you make?**
JWT in localStorage (simple, sensitive to XSS), a soft message rate limit so retries never use up quota, eventually consistent previews and unread counts, and an in-memory WebSocket broker that supports one server instance.

**Q66. What is the future scope?**
File and image sharing, push notifications, end-to-end encryption, voice channels, refresh tokens, and "delivered" receipts.

### Rapid-fire revision

| Question | One-line answer |
|---|---|
| Backend language and framework? | Java 17, Spring Boot 3.5 |
| Database? | MongoDB (Atlas in the cloud), 10 collections |
| Real-time technology? | WebSocket with the STOMP protocol |
| Password hashing? | BCrypt |
| Token type and lifetime? | JWT signed with HS256, valid 72 hours |
| Most important index? | `{roomId: 1, createdAt: -1}` on messages |
| How are duplicates stopped? | clientMessageId + unique index |
| Pagination type? | Cursor (before = createdAt), 30 per page |
| Server roles? | Owner, Admin, Member (RBAC per server) |
| Where are permissions checked? | AccessGuard in the service layer |
| Error format? | One JSON envelope: success, message, errorCode, requestId |
| No permission? | 403 Forbidden |
| Database down? | 503 DB_503 |
| Rate limit hit? | 429 RATE_429 |
| Redis down? | RedisSafe falls back, app keeps working |
| Number of tests? | 28, all passing |
| Frontend? | Next.js 16, React 19, TypeScript |
| 3D library? | Three.js via React Three Fiber |
| Hosting? | Vercel, Render (Docker), MongoDB Atlas, Upstash |
| Saved before or after delivery? | Before: save first, then deliver |

**Two-minute demo to offer:** log in as `razi` on the laptop and `aisha` on a phone, send a message, show typing and blue ticks, refresh MongoDB Compass to show the new document, then open a server to show roles and the member list.
