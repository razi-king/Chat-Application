# 2. Architecture and backend design

## Layered architecture
A request passes through clear layers. Each layer has **one job**.

```
Request
  -> RequestIdFilter          gives the request an id for the logs
  -> JwtAuthenticationFilter  checks the login token
  -> Controller               receives the request, validates the DTO
  -> Service (+ AccessGuard)  business rules + permission checks
  -> Repository               reads/writes MongoDB
  -> MongoDB
Errors from any layer -> RestResponseExceptionHandler -> standard JSON error
```

| Layer | Package | Job | Example |
|---|---|---|---|
| Controller | `controler/` | Receive REST / WebSocket calls, validate input with `@Valid` | `RoomControler`, `ChatController` |
| Service | `service/` + `service/impl/` | Business rules | `ChatServiceImpl.sendMessage` |
| AccessGuard | `security/AccessGuard` | "Is this user allowed?" in one place | `requireRoomMember`, `requireServerRole` |
| Repository | `repository/` | Database queries (Spring Data) | `MessageRepository` |
| Mapper | `mapper/` | Entity -> DTO, loads users in batches | `MessageMapper` |
| Entity | `entity/` | MongoDB documents | `Message`, `Room`, `User` |
| DTO | `dto/` | What goes in and out of the API | `SendMessageRequest`, `MessageResponse` |

**Why thin controllers?** The same `ChatService.sendMessage` is used by both the REST endpoint and the WebSocket endpoint. If the rules lived in controllers they would be written twice.

**Why DTOs?** They decide exactly which fields the API accepts and returns. The password hash never leaves the server, and a client cannot set fields like `senderId` (the sender always comes from the token).

## Two ways the frontend talks to the backend

| Channel | Used for | Example |
|---|---|---|
| REST over HTTPS | Request / response | Login, load chat history, edit profile |
| WebSocket (STOMP) | Live push from the server | New message, typing, read ticks, online status |

Sending a message uses the WebSocket first and falls back to REST if the socket is down.

## Design principles applied

| Principle | Where in the code | What it gives |
|---|---|---|
| Single Responsibility | `AccessGuard` only checks permissions; `MessageMapper` only maps | Easy to read and audit |
| Open/Closed | New error = one new `ErrorCode` constant | The error handler never changes |
| Liskov Substitution | All custom exceptions extend `ChatAppException` | One handler works for all |
| Interface Segregation | Small interfaces: `RateLimitService`, `PresenceService` | Classes depend only on what they use |
| Dependency Inversion | Services use `RealtimeService`, not the WebSocket class directly | Delivery can be swapped later |
| DRY | `Response.ok/error`, `AccessGuard`, `RedisSafe`, `ReusableForm` | Code written once |
| KISS / YAGNI | No microservices, no Kafka | Simple, enough for this scale |
| Dependency Injection | Constructor injection (`@RequiredArgsConstructor`) | Classes are easy to test with mocks |

## Example request flow: login
1. `POST /api/v1/auth/login` with username (or email) and password.
2. DTO validated (both fields required).
3. Rate limit checked: max 10 attempts per minute (Redis).
4. User found in MongoDB; password checked with BCrypt.
5. Server creates a JWT (user id, username, unique id, expiry 72 h).
6. Response: `{ token, expiresAt, user }`. The frontend saves the token and sends it on every request.

Wrong password or unknown user gives the **same** error (`AUTH_402`), so attackers cannot find out which usernames exist.
