# Deployment

## 1. Local development (recommended for the demo)

```bash
docker compose up -d                       # MongoDB 7 on 27017, Redis 7 on 6379
cd ChatAppBackend && ./mvnw spring-boot:run
cd chat-frontend && npm install && npm run dev
```
Open `http://localhost:3000`. Swagger: `http://localhost:8080/swagger-ui.html`.

**Without Docker:** install MongoDB Community locally; Redis is optional (the app logs `Redis unavailable, using fallback` once and keeps working).

**Reset demo data:** `docker exec nexus-mongo mongosh chatapp --eval "db.dropDatabase()"` then restart the backend.

## 1A. Free cloud deployment (share a link that opens on any phone)

| Part | Free service | Why this one |
|---|---|---|
| Frontend (Next.js) | **Vercel** (Hobby) | Made by the Next.js team; zero config; HTTPS + global CDN |
| Backend (Spring Boot) | **Render** (Free web service, Docker) | Runs our `Dockerfile`; supports WebSockets; 512 MB RAM. Sleeps after 15 min idle → ~1 min cold start |
| Database | **MongoDB Atlas** (M0 free cluster) | Managed MongoDB, 512 MB storage, same driver/URI style |
| Redis (optional) | **Upstash** (free) — or skip it | App runs without Redis thanks to `RedisSafe` fallbacks |

Total cost: ₹0. All four need only a GitHub/Google sign-up.

**Step 1 — Push the code to GitHub** (repo `razi-king/Chat-Application`). Make sure `chat-frontend/.env` is NOT committed (it is git-ignored).

**Step 2 — MongoDB Atlas**
1. cloud.mongodb.com → create a free **M0** cluster.
2. *Database Access* → add a user (username + password).
3. *Network Access* → add `0.0.0.0/0` (Render's IPs change, so allow all; the DB password still protects it).
4. *Connect → Drivers* → copy the URI and add the database name: `mongodb+srv://USER:PASS@cluster0.xxxx.mongodb.net/chatapp?retryWrites=true&w=majority`.

**Step 3 — Backend on Render**
1. render.com → *New → Blueprint* → select the GitHub repo (it reads `render.yaml`). Or *New → Web Service* → Docker, root directory `ChatAppBackend`.
2. Fill the environment variables: `MONGODB_URI` (from step 2), `SEED_PASSWORD` (demo password), `CORS_ORIGINS` = `https://*.vercel.app` for now (tighten after step 4), optional `SPRING_DATA_REDIS_URL` (Upstash `rediss://...`). `JWT_SECRET` is generated automatically.
3. Deploy (first build ≈ 5–10 min). Test `https://<your-service>.onrender.com/api/v1/health` → `{"status":"UP"}`. On the first start the demo data is created in Atlas.

**Step 4 — Frontend on Vercel**
1. vercel.com → *Add New → Project* → import the repo → **Root Directory: `chat-frontend`** (framework auto-detected: Next.js).
2. Environment variables (must exist **before** the build, they are baked into the bundle):
   * `NEXT_PUBLIC_API_BASE_URL` = `https://<your-service>.onrender.com/api/v1`
   * `NEXT_PUBLIC_WS_URL` = `wss://<your-service>.onrender.com/ws`  (note **wss**, not ws)
3. Deploy → you get `https://<project>.vercel.app`.
4. Back on Render, set `CORS_ORIGINS=https://<project>.vercel.app` (exact URL is safer than the wildcard) → Render redeploys.

**Step 5 — Share:** send the Vercel link (or show it as a QR code) — it works on any phone, no install.

**Before the demo:** open the site ~2 minutes earlier. The landing page pings `/api/v1/health`, which wakes the sleeping Render service; once WebSockets are connected, the activity keeps it awake.

**Troubleshooting**
| Symptom | Cause / fix |
|---|---|
| "Can't reach the server…" on first try | Render was asleep — wait ~1 min, retry |
| Browser console: CORS error | `CORS_ORIGINS` on Render doesn't match the Vercel URL exactly (https, no trailing slash) |
| Messages only appear after refresh | `NEXT_PUBLIC_WS_URL` wrong (must be `wss://…/ws`) → fix on Vercel and **redeploy** |
| Render logs: `MongoTimeoutException` | Atlas Network Access missing `0.0.0.0/0`, or wrong password in the URI (URL-encode special characters) |
| Render logs: `Redis unavailable, using fallback` | Expected when no Redis is configured — app keeps working |

## 2. Building artifacts

```bash
# Backend: executable jar
cd ChatAppBackend && ./mvnw clean package          # target/ChatAppBackend-0.0.1-SNAPSHOT.jar
java -jar target/ChatAppBackend-0.0.1-SNAPSHOT.jar

# Frontend: optimised build
cd chat-frontend && npm run build && npm start      # serves on :3000
```

## 3. Production configuration

| Setting | Value |
|---|---|
| `JWT_SECRET` | ≥ 32 random bytes, from a secret manager |
| `MONGODB_URI` | replica set URI with auth, e.g. `mongodb+srv://user:pass@cluster/chatapp` |
| `REDIS_HOST` / `REDIS_PORT` | managed Redis with password/TLS |
| `CORS_ORIGINS` | `https://chat.yourcollege.edu` |
| `SEED_DEMO_DATA` | `false` |
| Frontend `NEXT_PUBLIC_API_BASE_URL` / `NEXT_PUBLIC_WS_URL` | `https://api.../api/v1`, `wss://api.../ws` (set **before** `npm run build`; they are baked into the bundle) |

## 4. Reverse proxy (nginx example)

WebSocket needs the `Upgrade` headers forwarded and a long read timeout:

```nginx
location /ws {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600s;
}
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header X-Request-Id $request_id;   # correlation id from the edge
}
```

## 5. Multi-instance notes

The in-memory STOMP broker works for **one** backend instance. Before running two or more:
1. Switch to a broker relay (RabbitMQ STOMP plugin) or Redis pub/sub fan-out (see WEBSOCKET-DESIGN.md §6).
2. Use sticky sessions for `/ws` at the load balancer.
3. Redis becomes **required** (presence and rate limits must be shared); remove reliance on the in-memory fallback.

## 6. Operations checklist

* Health: `GET /api/v1/health` (add Spring Boot Actuator for `/actuator/health` + metrics in production).
* Logs: every line has `[req:<id>]`; ship them to a log store (ELK/Loki) and search by request id.
* Backups: MongoDB daily snapshots; Redis data is rebuildable (presence resets, unread falls back to MongoDB).
* Index check after deploy: `db.messages.getIndexes()` must list `room_created` and `idempotency_key`.
