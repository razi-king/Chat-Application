# Nexus — College Chat Application

A real-time chat platform that mixes **WhatsApp-style** private chats and groups with **Discord-style** servers and channels.
Built with **Spring Boot 3 (Java 17)**, **MongoDB**, **Redis**, **STOMP WebSockets** and a **Next.js 16 / React 19 / Three.js** frontend.

> This README is the entry point. Every design decision is explained in [`docs/`](docs/). If you are new to the code, read [`docs/LEARNING-GUIDE.md`](docs/LEARNING-GUIDE.md) first. If you are preparing for a viva, read [`docs/VIVA-QUESTIONS.md`](docs/VIVA-QUESTIONS.md).

---

## 1. Features

| Area | What works |
|---|---|
| Accounts | Register, login (username **or** email), logout (token revoked), edit profile (name, status, about, avatar colour) |
| People | Search users, friend requests (send / accept / decline / remove), online / last-seen presence |
| WhatsApp side | 1-to-1 chats, groups with owner/admin/member roles, add/remove members, leave group, read receipts (✓ / blue ✓✓), typing indicator |
| Discord side | Servers, text channels, roles (owner/admin/member), kick, promote, invite links with expiry, public "Discover" list, audit log |
| Messages | Real-time delivery, history with cursor pagination (infinite scroll), replies, reactions, edit, delete, pin, search, @mentions |
| Reliability | Idempotent sends (no duplicates on retry), WebSocket → REST fallback, auto-reconnect, rate limiting, Redis fallback when Redis is down |
| Errors | One response envelope for every API and WebSocket error, typed error codes, field-level validation messages, correlation ids |
| UI | 3D landing / auth scenes (React Three Fiber), glassmorphism dark theme, responsive layout, loading / empty / error states |

## 2. Technology stack

| Layer | Technology | Why (short) — full reasoning in [DECISIONS.md](docs/DECISIONS.md) |
|---|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Validation, Security, Data MongoDB, WebSocket) | Mature, layered, dependency injection, huge ecosystem |
| Database | MongoDB 7 | Chat data is document-shaped and append-heavy; flexible schema for reactions/mentions |
| Cache / counters | Redis 7 (optional at runtime) | Atomic counters for presence, unread badges, rate limits, logout blacklist |
| Real-time | STOMP over WebSocket | Server push with topics + per-user queues, built into Spring |
| Auth | JWT (jjwt) + BCrypt | Stateless auth that works for both REST and WebSocket |
| API docs | springdoc OpenAPI / Swagger UI | Live, try-it-out documentation |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind 4, Formik + Yup, @stomp/stompjs, Three.js / React Three Fiber, framer-motion | Typed, component-based, modern UI |
| Tests | JUnit 5, Mockito, MockMvc, Spring Boot Test | Unit + error-flow + concurrency + real-MongoDB integration tests |
| Dev infra | Docker Compose (MongoDB + Redis) | One command to start the databases |

## 3. Architecture at a glance

```
 Browser (Next.js)
   │  REST  (axios, JWT in Authorization header)
   │  WebSocket /ws  (STOMP, JWT in CONNECT frame)
   ▼
 Spring Boot
   RequestIdFilter ─► JwtAuthenticationFilter ─► Spring Security
   │                                    WebSocketAuthInterceptor (CONNECT / SUBSCRIBE)
   ▼
 Controllers (controler/)  ── DTOs in / DTOs out, @Valid
   ▼
 Services (service/ + service/impl/)  ── business rules, AccessGuard permission checks
   │            │                       │
   ▼            ▼                       ▼
 Repositories  RealtimeService         Redis services (presence, unread, rate limit, blacklist)
 (MongoDB)     (SimpMessagingTemplate)  via RedisSafe (fallback when Redis is down)
```

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 4. Prerequisites

* Java **17+** (tested with 24), no Maven install needed (the `mvnw` wrapper downloads it)
* Node.js **20+** (tested with 22) and npm
* Docker Desktop (for MongoDB + Redis) — or a local MongoDB 6+/7 and (optionally) Redis 7

## 5. Run it locally

```bash
# 1. Databases (from the project root)
docker compose up -d

# 2. Backend  -> http://localhost:8080   (Swagger: http://localhost:8080/swagger-ui.html)
cd ChatAppBackend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend -> http://localhost:3000
cd chat-frontend
npm install
npm run dev
```

On the **first start with an empty database** the backend creates demo data (5 users, 2 servers, a group, a DM, invite code `NEXUSHQ1`).
The demo usernames are `razi`, `aisha`, `kabir`, `zoya`, `dev`; the shared demo password is the value of `app.seed.password` in
`ChatAppBackend/src/main/resources/application.properties` (overridable with the `SEED_PASSWORD` environment variable).
Tip: log in as two different users in two browsers (or one normal + one private window) to watch real-time delivery, typing and read ticks.

## 6. Environment variables

### Backend (all optional — defaults are for local development)

| Variable | Default | Purpose |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/chatapp` | MongoDB connection string |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis location (app still works if Redis is down) |
| `JWT_SECRET` | dev-only value | HMAC key for signing JWTs — **must** be a long random value in production |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed frontend origin(s), comma separated |
| `SEED_DEMO_DATA` | `true` | Create demo data on an empty database |
| `SEED_PASSWORD` | see properties file | Password of every demo account |
| `APP_TIMEZONE` | `Asia/Kolkata` | Timezone of the `timestamp` field in responses |

### Frontend (`chat-frontend/.env`)

| Variable | Default | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api/v1` | REST base URL (must start with `NEXT_PUBLIC_` to reach the browser) |
| `NEXT_PUBLIC_WS_URL` | `ws://localhost:8080/ws` | STOMP WebSocket endpoint |

## 7. MongoDB setup

Nothing manual: `spring.data.mongodb.auto-index-creation=true` creates all collections' indexes on startup.
The 10 collections are `users, servers, server_members, rooms, room_members, messages, invites, friendships, notifications, audit_logs`.
Schema, indexes and query patterns: [docs/DATABASE-DESIGN.md](docs/DATABASE-DESIGN.md).

## 8. API & WebSocket overview

* REST base path: `/api/v1` — full endpoint table in [docs/API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md)
* Every response uses one envelope:

```json
{ "success": true, "data": { }, "timestamp": "02-10-2026 15:52:44", "message": "Message Sent" }
{ "success": false, "data": null, "timestamp": "...", "message": "Room not found", "errorCode": "ROOM_404", "requestId": "9f1c2a7b3d4e5f60" }
```

* WebSocket: connect to `/ws` with `Authorization: Bearer <jwt>` in the STOMP CONNECT headers, then
  * subscribe `/topic/rooms/{roomId}` (messages, typing, read receipts), `/user/queue/events` (unread, notifications), `/user/queue/errors`, `/topic/presence`
  * send to `/app/rooms/{roomId}/send` and `/app/rooms/{roomId}/typing`
  * details in [docs/WEBSOCKET-DESIGN.md](docs/WEBSOCKET-DESIGN.md)

## 9. Tests

```bash
cd ChatAppBackend
./mvnw test
```

28 tests: unit (Mockito), error-flow (MockMvc), JWT, a concurrency test, and real-MongoDB integration tests (auto-skipped if MongoDB is not running).
See [docs/TESTING.md](docs/TESTING.md).

## 10. Project structure

```
Chat-Application/
├── docker-compose.yml           MongoDB + Redis for development
├── docs/                        All design documentation
├── ChatAppBackend/              Spring Boot API
│   └── src/main/java/com/example/crm/
│       ├── config/              Security, WebSocket, request-id filter, demo seeder
│       ├── controler/           REST + STOMP controllers (thin)
│       ├── dto/                 Response envelope, request/response/event DTOs
│       ├── entity/              MongoDB documents (10 collections)
│       ├── enums/               ErrorCode, roles, room types, event types ...
│       ├── exception/           Custom exception hierarchy + global handler
│       ├── mapper/              Entity -> DTO conversion (batch loading)
│       ├── payload/             Incoming WebSocket payloads
│       ├── repository/          Spring Data MongoDB repositories
│       ├── security/            JWT, auth filter, AccessGuard (permissions)
│       ├── service/ (+ impl/)   Business logic behind interfaces
│       └── util/                RedisSafe, colour palette
└── chat-frontend/               Next.js app
    ├── app/                     Routes (landing, auth, /app shell, chats, servers ...)
    ├── component/               UI: chat window, sidebars, forms, 3D scenes, enums of styles
    ├── context/                 Auth, Socket, AppState, Server React contexts
    ├── lib/                     ApiError, errorHandler, utils
    ├── services/                API clients (one class per backend area)
    └── types/                   TypeScript mirrors of backend DTOs
```

## 11. Documentation map

| Document | Read it to understand |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, request flows, SOLID/DRY/KISS in this code, consistency, concurrency, scaling |
| [DATABASE-DESIGN.md](docs/DATABASE-DESIGN.md) | Every collection, sample documents, indexes, query patterns, pagination |
| [API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md) | Every endpoint: auth, request, validation, response, errors, authorization |
| [SECURITY.md](docs/SECURITY.md) | Passwords, JWT, WebSocket auth, authorization, logging hygiene |
| [WEBSOCKET-DESIGN.md](docs/WEBSOCKET-DESIGN.md) | Message flow, offline users, reconnects, persistence vs delivery |
| [ERROR-HANDLING.md](docs/ERROR-HANDLING.md) | Exception hierarchy, error codes, error flows front to back |
| [TESTING.md](docs/TESTING.md) | What each test protects against |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Running in production, configuration, checklist |
| [LEARNING-GUIDE.md](docs/LEARNING-GUIDE.md) | A 24-step path from "project structure" to "scalability" |
| [DECISIONS.md](docs/DECISIONS.md) | Architectural decision records (why X, not Y) |
| [VIVA-QUESTIONS.md](docs/VIVA-QUESTIONS.md) | Questions your professor / interviewer is likely to ask, with answers |
| [DEMO-CHECKLIST.md](docs/DEMO-CHECKLIST.md) | One-page live demo script for the presentation |

## 12. Future improvements

File/image attachments (object storage + pre-signed URLs), push notifications, end-to-end encryption for DMs,
refresh tokens, message delivery receipts (✓✓ grey = delivered), voice channels (WebRTC), a Redis-backed STOMP broker relay for multi-instance deployments,
and a moderation dashboard. The scaling path is described in [ARCHITECTURE.md §9](docs/ARCHITECTURE.md#9-scaling-from-100-to-1-million-users).
