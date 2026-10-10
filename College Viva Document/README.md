# Nexus: Viva Notes

**Razi Ahmed · Roll No. 57 · Master Data Analytics · Guide: Prof. Omkar Sherkhane**

Read these in order. Each file explains one part of the project in simple words, including **what** each part does, **why** it was built that way, and **where** the code is. The same content, combined and formatted, is in [Nexus_Project_Documentation.pdf](Nexus_Project_Documentation.pdf).

| # | File | What it covers |
|---|---|---|
| 1 | [01-overview.md](01-overview.md) | The problem, the idea, features, the whole system in one picture |
| 2 | [02-architecture.md](02-architecture.md) | Layers (controller, service, repository), design principles, request flow |
| 3 | [03-database-mongodb.md](03-database-mongodb.md) | The 10 MongoDB collections, indexes, pagination, consistency |
| 4 | [04-realtime-messaging.md](04-realtime-messaging.md) | WebSocket + STOMP, sending a message step by step, offline users, no duplicates |
| 5 | [05-security-and-rbac.md](05-security-and-rbac.md) | BCrypt, JWT login, logout, WebSocket security, Owner/Admin/Member roles |
| 6 | [06-redis-and-error-handling.md](06-redis-and-error-handling.md) | What Redis stores, fallback when Redis is down, custom exceptions, error codes |
| 7 | [07-backend-api.md](07-backend-api.md) | REST endpoints, request/response examples, status codes |
| 8 | [08-frontend.md](08-frontend.md) | Next.js pages, contexts, 3D scenes, mobile layout |
| 9 | [09-testing-and-deployment.md](09-testing-and-deployment.md) | The 33 tests, bugs they found, Docker, free cloud deployment, scaling |
| 10 | [10-viva-questions.md](10-viva-questions.md) | 66 likely viva questions with short answers + rapid-fire table |
| 11 | [11-live-demo-script.md](11-live-demo-script.md) | Step-by-step live demo: each action in the app → which collections change in Compass → what Redis shows |

**Key numbers to remember**
- **10** MongoDB collections; most important index **{roomId: 1, createdAt: -1}** (latest 30 messages in one index walk)
- **3** roles per server: Owner > Admin > Member (RBAC enforced by `AccessGuard`)
- JWT valid **72 hours**, passwords hashed with **BCrypt**
- Rate limits: **8 messages / 5 s**, **10 logins / minute**, **5 wrong passwords = account locked 15 min** (Redis)
- **33** automated tests, all passing; tests found and fixed **2** real race-condition bugs
- Idempotency proven: **10** parallel retries of one message → **1** stored document
- Deployed for free: Vercel + Render (Docker, Singapore) + MongoDB Atlas + Upstash Redis; backend uses about **162 MB of 512 MB**
- Free server sleeps after **15 min** idle → about **1 min** cold start

The deeper engineering docs (for reference, longer) are in the project's [`docs/`](../docs/) folder.
