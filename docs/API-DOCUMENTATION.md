# API Documentation

* **Base URL:** `http://localhost:8080/api/v1`
* **Live docs / try-it:** `http://localhost:8080/swagger-ui.html` (click **Authorize**, paste the JWT)
* **Auth:** `Authorization: Bearer <jwt>` on every endpoint marked 🔒
* **Content type:** `application/json`
* **Correlation id:** every response has an `X-Request-Id` header; send your own (8–64 chars `[a-zA-Z0-9-]`) to trace a call

## 1. Response envelope (every endpoint)

```json
// success
{ "success": true,  "data": { },  "timestamp": "02-10-2026 15:52:44", "message": "Messages" }
// failure
{ "success": false, "data": null, "timestamp": "02-10-2026 15:52:46", "message": "Please fix the highlighted fields",
  "errorCode": "VAL_400", "requestId": "0912d6729bd746a6",
  "errorMeta": { "clientMessageId": "clientMessageId may only contain letters, digits, - and _" } }
```
`errorMeta` appears only for field-level problems; `requestId` only on errors.

## 2. Status codes used

| Status | Meaning in this API | Example codes |
|---|---|---|
| 200 / 201 | OK / created | — |
| 400 | Invalid input or business rule violated | `VAL_400`, `REQ_400`, `SRV_410`, `FRD_400` |
| 401 | Not logged in / bad / expired / revoked token / wrong credentials | `AUTH_401`, `AUTH_402`, `AUTH_403` |
| 403 | Logged in but not allowed | `AUTH_404`, `ROOM_403`, `SRV_403`, `MSG_403` |
| 404 | Resource does not exist | `USER_404`, `ROOM_404`, `SRV_404`, `MSG_404`, `INV_404`, `REQ_404` |
| 409 | Conflict / duplicate | `USER_409`, `USER_410`, `ROOM_409`, `FRD_409`, `REQ_409` |
| 410 | Invite expired or used up | `INV_410` |
| 429 | Rate limited | `RATE_429` |
| 500 | Unexpected bug (details only in logs) | `SYS_500` |
| 503 | Database temporarily unavailable — retry | `DB_503` |

Common to every 🔒 endpoint: `401 AUTH_401` (no/invalid token), `401 AUTH_403` (expired or logged-out token), `503 DB_503`.

## 3. Auth — `AuthController`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Create account, returns a token |
| POST | `/auth/login` | public | Log in with username **or** email |
| POST | `/auth/logout` | 🔒 | Revoke the current token (Redis blacklist until it expires) |
| GET | `/auth/me` | 🔒 | Current user incl. email |

**POST /auth/register**
```json
{ "username": "razi_k", "email": "razi@example.com", "password": "secret123", "displayName": "Razi Khan" }
```
Validation: username 3–20 chars `[a-zA-Z0-9_.]`, valid email, password 6–64, displayName ≤ 40, all required. Username/email are lower-cased.
Response `201`: `{ token, expiresAt, user }`. Errors: `400 VAL_400`, `409 USER_409` (`errorMeta.username`), `409 USER_410` (`errorMeta.email`).

**POST /auth/login** `{ "identifier": "razi", "password": "..." }` → `200 { token, expiresAt, user }`.
Errors: `401 AUTH_402` for both unknown user and wrong password (deliberately identical), `429 RATE_429` after 10 attempts/minute per identifier.

## 4. Users — `UserController` 🔒

| Method | Path | Purpose | Notes |
|---|---|---|---|
| GET | `/users/me` | My profile | includes email |
| PUT | `/users/me` | Update my profile | `{displayName?, about?, customStatus?, avatarColor?}`; ≤40/≤140/≤60 chars, colour `#rrggbb`. Only ever edits the caller (id from token) |
| GET | `/users/me/dashboard` | Counters for the home screen | friends, pending requests, servers, conversations, unread messages/notifications, online friends |
| GET | `/users/search?q=` | Find people by username / display name | top 20, excludes me |
| GET | `/users/{userId}` | Public profile | `404 USER_404`; never includes email |

## 5. Rooms & messages — `RoomControler` 🔒

| Method | Path | Purpose | Authorization | Errors |
|---|---|---|---|---|
| GET | `/rooms` | My DMs + groups (chat list) with unread counts, last message, members | — | — |
| GET | `/rooms/{roomId}` | Room details | member | 404 ROOM_404, 403 ROOM_403 |
| POST | `/rooms/direct` | Open (or create) a DM `{ "userId": "..." }` | — | 400 (self), 404 USER_404 |
| POST | `/rooms/group` | Create group `{ name, description?, memberIds[] }` | — | 400 VAL_400 |
| DELETE | `/rooms/{roomId}` | Delete group (owner) or channel (server admin) | as stated | 400 (DM / last channel), 403 |
| POST | `/rooms/{roomId}/members` | Add members `{ userIds[] }` (groups) | group admin+ | 400 (not a group), 403 |
| DELETE | `/rooms/{roomId}/members/{userId}` | Remove member, or leave if `userId` = me | admin+ and target lower role, or self | 403 |
| POST | `/rooms/{roomId}/read` | Mark read → read receipts + clear badge | member | 403 |
| GET | `/rooms/{roomId}/messages?before=&size=30` | History, newest page first; `before` = ISO `createdAt` of the oldest loaded message | member | 403 |
| POST | `/rooms/{roomId}/messages` | Send (REST fallback) | member | 400, 403, 429 |
| GET | `/rooms/{roomId}/pinned` | Pinned messages | member | 403 |
| GET | `/rooms/{roomId}/search?q=` | Search in room (top 50) | member | 403 |

**POST /rooms/{roomId}/messages**
```json
{ "content": "See you at 5 @kabir", "replyToId": "6abf...", "clientMessageId": "c-3f1d9a0e-8c2b-4f6e" }
```
Validation: content 1–2000 chars, `clientMessageId` optional ≤64 `[a-zA-Z0-9_-]`, `replyToId` must be a message of the same room.
**Idempotent:** sending the same `clientMessageId` again returns the already stored message (same `id`), no duplicate, no extra unread badge.
Response `201`: `MessageResponse`
```json
{ "id": "...", "roomId": "...", "clientMessageId": "c-3f1d...", "sender": { "id": "...", "displayName": "Razi Khan", "online": true, ... },
  "content": "See you at 5 @kabir", "type": "TEXT", "replyTo": { "id": "...", "senderName": "Kabir", "content": "..." },
  "reactions": [ { "emoji": "🔥", "count": 2, "userIds": ["..",".."] } ], "mentions": ["..."],
  "edited": false, "deleted": false, "pinned": false, "createdAt": "2026-10-02T10:22:46.120Z" }
```
**GET /rooms/{id}/messages** → `{ items: MessageResponse[] (oldest→newest), page: 0, size: 30, hasNext: true }`.

## 6. Message actions — `ChatController` 🔒

| Method | Path | Purpose | Authorization |
|---|---|---|---|
| PUT | `/messages/{id}` | Edit `{content}` | sender only (`403 MSG_403`) |
| DELETE | `/messages/{id}` | Soft delete | sender, or moderator (server admin / group admin) |
| POST | `/messages/{id}/reactions` | Toggle reaction `{emoji}` | room member |
| POST | `/messages/{id}/pin` | Toggle pin | any member in DM/group; admin+ in channels |

All return the updated `MessageResponse` and broadcast `MESSAGE_UPDATED` / `MESSAGE_DELETED` to the room.

## 7. Servers — `ServerController` 🔒

| Method | Path | Purpose | Authorization |
|---|---|---|---|
| GET | `/servers` | My servers (role, member count, unread) | — |
| POST | `/servers` | Create `{name, description?, iconColor?, discoverable}` → creates #general, #announcements, #off-topic | — |
| GET | `/servers/discover` | Public servers | — |
| GET | `/servers/{id}` | Detail: server + channels (with unread) + members (with presence) | member |
| PUT | `/servers/{id}` | Update | admin+ |
| DELETE | `/servers/{id}` | Delete with all channels/messages | owner |
| POST | `/servers/{id}/join` | Join a public server | — (`403` if invite-only) |
| POST | `/servers/{id}/leave` | Leave | member, not owner (`400 SRV_410`) |
| DELETE | `/servers/{id}/members/{userId}` | Kick | admin+, only lower roles |
| PUT | `/servers/{id}/members/{userId}/role` | `{role: ADMIN|MEMBER}` | owner |
| POST | `/servers/{id}/channels` | Create channel `{name (lowercase-dashes), description?}` | admin+ (`409 ROOM_409` duplicate name) |
| POST | `/servers/{id}/invites` | Create invite `{maxUses 0–1000, expiresInHours 0–720}` (0 = unlimited/never) | member |
| GET | `/servers/{id}/invites` | List invites | admin+ |
| GET | `/servers/{id}/audit-logs?page=&size=` | Audit log | admin+ |

## 8. Social & invites — `SocialController`

| Method | Path | Auth | Purpose | Errors |
|---|---|---|---|---|
| GET | `/friends` | 🔒 | Friends + pending requests (`incoming` flag) | — |
| POST | `/friends/requests` | 🔒 | `{username}`; if they already asked you, this accepts | 404, 400 FRD_400 (self), 409 FRD_409 |
| POST | `/friends/{id}/accept` | 🔒 | Accept (addressee only) | 403, 404 |
| DELETE | `/friends/{id}` | 🔒 | Decline / cancel / unfriend (either party) | 403, 404 |
| GET | `/notifications?page=&size=` | 🔒 | Inbox, newest first | — |
| GET | `/notifications/unread-count` | 🔒 | `{count}` | — |
| POST | `/notifications/{id}/read` | 🔒 | Mark one read (own only → else 404) | 404 |
| POST | `/notifications/read-all` | 🔒 | Mark all read | — |
| GET | `/invites/{code}` | public | Invite preview (server name, members, inviter) | 404 INV_404, 410 INV_410 |
| POST | `/invites/{code}/accept` | 🔒 | Join via invite (idempotent: already a member → just returns the server) | 404, 410 |
| GET | `/health` | public | `{status: "UP"}` | — |

## 9. Design rules followed

* Resources are nouns, actions on a resource are sub-paths (`/messages/{id}/pin`), the version is in the path (`/v1`).
* IDs are opaque strings. Internal fields (`passwordHash`, `directKey`, `pairKey`) are never returned, and `email` appears only on your own profile (`/auth/me`, `/users/me`).
* The acting user is **always** taken from the JWT — no endpoint accepts "userId of the actor" in the body.
* `PUT` replaces/edits, `POST` creates or performs an action, `DELETE` removes. Re-running the "join"/"open DM"/"accept invite"/"send with clientMessageId" operations is safe (idempotent).
