# Nexus — Live Demo Checklist (one page)

**Before the professor arrives (5 min):**
☐ Docker Desktop running → `docker compose up -d` (MongoDB + Redis)
☐ Backend: `cd ChatAppBackend` → `.\mvnw.cmd spring-boot:run` (wait for "Started ChatAppBackendApplication")
☐ Frontend: `cd chat-frontend` → `npm run dev` → open http://localhost:3000 once (first load compiles)
☐ Open: **Browser A** (normal) · **Browser B** (private/incognito window) · **MongoDB Compass** (`mongodb://localhost:27017` → `chatapp`) · **RedisInsight** (`localhost:6379`) · Swagger (http://localhost:8080/swagger-ui.html)
☐ Demo users: `razi` (OWNER of Nexus HQ), `aisha` (ADMIN), `kabir`, `zoya` (MEMBERs) · password = `app.seed.password` in `application.properties`

| # | Show | Do this | Point out | ⏱ |
|---|---|---|---|---|
| 1 | **Intro + 3D UI** | Landing page, scroll | "WhatsApp + Discord. Spring Boot, MongoDB, Redis, WebSocket, Next.js, Three.js" | 1 min |
| 2 | **Error handling** | Login with a wrong password | Toast + red field error. Backend sent `AUTH_402` in the standard `Response` envelope | 30 s |
| 3 | **Login / JWT** | A: login `razi`; B: login `aisha` | BCrypt check → JWT token → WebSocket connects ("LIVE" dot). Compass `users`: `passwordHash` is hashed | 1 min |
| 4 | **Dashboard** | Razi's home | Unread counts come from **Redis** | 20 s |
| 5 | **Real-time DM** | A opens chat with Aisha, sends "Hello professor" | Appears instantly in B. Compass `messages` → Refresh → new document | 1 min |
| 6 | **Typing + read ticks** | B types (A sees "typing…"), B opens the chat | A's ✓ turns blue ✓✓. Compass `room_members.lastReadAt` changed | 1 min |
| 7 | **Message features** | Reply, 🔥 react, edit, delete, pin | Compass: `reactions`, `edited:true`, `deleted:true` (soft delete) | 1 min |
| 8 | **Group** | Open "Exam Squad", send `@kabir hi` | Group roles. `notifications` collection gets a MENTION | 30 s |
| 9 | **Discord server** | Open **Nexus HQ** → #general | Channels, member list grouped by role, online dots | 30 s |
| 10 | **RBAC** | B (Aisha=ADMIN): can create channel, kick Zoya, **can't** kick Razi. Log in as `kabir` (MEMBER): no "Create channel" button; same call in Swagger → **403** | "Roles are per server, enforced in the backend by `AccessGuard`, not just hidden buttons" | 2 min |
| 11 | **Invite** | Server menu → Invite → copy link → open in another login | Join flow, invite stored in `invites` | 30 s |
| 12 | **Audit log** | Razi: server menu → Audit log | Who did what (kick, role change…), `audit_logs` collection | 20 s |
| 13 | **Redis live** | RedisInsight: `presence:connections`, `unread:<userId>` | Open/close tab → count changes. Send msg → unread +1, open chat → removed | 1 min |
| 14 | **Redis down** | `docker stop nexus-redis`, send a message | Still works (fallback to MongoDB). `docker start nexus-redis` | 30 s |
| 15 | **Indexes** | Compass → `messages` → Indexes + Explain Plan | `IXSCAN` on `room_created`. Unique `idempotency_key` = no duplicate messages | 1 min |
| 16 | **Tests + docs** | `.\mvnw.cmd test` → 28 passing; show `docs/` folder | Unit, error-flow, concurrency, real-MongoDB tests | 1 min |

**Phone demo (if deployed):** open the Vercel link 2 min early (wakes the free server) → give the professor the link / QR code → they log in as `aisha` on their phone while you are `razi` on the laptop → chat live between the two devices. Mobile layout: bottom tabs, tap a message for actions.

**If something breaks:** say *"this shows the error handling"*, point to the toast/error code, refresh. Check Docker is running, then that ports 8080/3000 are up.

**3 sentences to end with:**
1. "MongoDB is the source of truth; every hot query has an index, and unique indexes stop duplicates and race conditions."
2. "Messages are saved first and delivered second over WebSocket, so offline users never lose messages."
3. "Redis handles fast counters (online, unread, rate limit, logout), and the app keeps working if Redis goes down."
