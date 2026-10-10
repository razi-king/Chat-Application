# 9. Testing and deployment

## Testing
Run with `./mvnw test` in `ChatAppBackend`: **33 tests, all passing** (2 MongoDB integration tests skip automatically when MongoDB is not running).

| Test class | Type | What it protects against |
|---|---|---|
| `AuthServiceImplTest` (6) | Unit (Mockito) | Plain-text passwords, duplicate accounts, revealing which usernames exist, brute force |
| `ChatServiceImplTest` (9) | Unit (Mockito) | Non-members posting, duplicate messages, spam, quoting another room's message, delivering a message that was never saved |
| `RestResponseExceptionHandlerTest` (6) | Web (MockMvc) | Wrong status codes, missing field errors, leaking internal details on 500/503 |
| `JwtServiceTest` (4) | Unit | Forged tokens, expired tokens, broken tokens |
| `PresenceServiceConcurrencyTest` (1) | Concurrency | 64 parallel connections must give exactly one online and one offline event |
| `ChatAppBackendApplicationTests` (2) | Integration (real MongoDB) | 10 parallel retries of one message must store exactly 1 document |

**Unit test vs integration test:** a unit test checks one class alone with fake (mocked) dependencies and runs in milliseconds. An integration test runs the real application with a real database to prove things a mock cannot, like the unique index really blocking duplicates.

**Bugs the tests found (and I fixed):**
1. **Presence race:** a reconnect happening during cleanup could be erased, so a user looked offline while online. Fixed with an atomic `computeIfPresent`.
2. **Rate limit vs retries:** 10 parallel retries of the same message used up the rate limit, so some got 429. Now a message counts only after a successful, non-duplicate save.

## Deployment (free)

| Part | Service | Notes |
|---|---|---|
| Frontend | Vercel | Built from `chat-frontend`, served over HTTPS from a CDN |
| Backend | Render (free web service) | Docker container, Singapore region |
| Database | MongoDB Atlas (M0 free) | Same 10 collections; viewable in MongoDB Compass |
| Cache | Upstash Redis (free) | Optional; app works without it |

**Docker:** a two-stage `Dockerfile`. Stage 1 builds the jar with Maven; stage 2 runs it on a small Java 17 runtime as a **non-root** user, with memory limits for the 512 MB free tier. It runs in about **162 MB**.

**Configuration:** all secrets are **environment variables** (`MONGODB_URI`, `JWT_SECRET`, `CORS_ORIGINS`, `SPRING_DATA_REDIS_URL`, `SEED_PASSWORD`), never in GitHub.

**Cold start:** Render's free server sleeps after 15 minutes without traffic and needs about 1 minute to wake. The website pings `/api/v1/health` as soon as a page opens so it starts waking early. Open the site a few minutes before a demo.

## How it would scale

| Users | Change needed |
|---|---|
| 100 (college) | Current design is enough |
| 10,000 | Always-on Redis, MongoDB replica set, monitoring |
| 100,000 | Several backend servers behind a load balancer; shared WebSocket broker (RabbitMQ or Redis pub/sub); only `RealtimeService` changes |
| 1,000,000 | Shard `messages` by roomId, separate WebSocket gateway, message queue (Kafka) for notifications and search, archive old messages |

## Future scope
File and image sharing, push notifications, end-to-end encryption, voice channels, refresh tokens, "delivered" (grey double tick) receipts.
