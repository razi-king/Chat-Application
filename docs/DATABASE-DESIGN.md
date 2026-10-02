# Database Design (MongoDB)

The schema was designed **from the queries backwards**: first list what the screens need, then shape documents and indexes so each
of those queries hits an index. Entities are in `ChatAppBackend/src/main/java/com/example/crm/entity/`.
Indexes are declared with `@Indexed` / `@CompoundIndex` and created at startup (`spring.data.mongodb.auto-index-creation=true`).

## 1. Collections overview

```
users ─┬─< server_members >── servers ──< invites
       │                         │
       │                         └──< audit_logs
       ├─< room_members >── rooms ──< messages
       │                     (DIRECT | GROUP | CHANNEL; CHANNEL has serverId)
       ├─< friendships (requester, addressee)
       └─< notifications
```
`─<` = "one to many by reference (stores the other document's id)".

| # | Collection | Purpose | Grows with |
|---|---|---|---|
| 1 | `users` | Accounts and profiles | users |
| 2 | `servers` | Discord-style communities | servers |
| 3 | `server_members` | Who is in which server, with which role | users × servers |
| 4 | `rooms` | Every conversation: DM, group, or channel | conversations |
| 5 | `room_members` | Who is in which room + read pointer | users × rooms |
| 6 | `messages` | Chat messages | **fastest growing** |
| 7 | `invites` | Server invite codes | invites |
| 8 | `friendships` | Friend requests / friendships | relations |
| 9 | `notifications` | Per-user notification inbox | events |
| 10 | `audit_logs` | Moderation history per server | admin actions |

## 2. Embedded vs referenced — the main decisions

| Data | Choice | Why |
|---|---|---|
| Messages inside room | **Referenced** (own collection) | Unbounded growth (16 MB document limit), every send would rewrite the whole room (lost updates under concurrency), and pagination over an embedded array is awkward. This was the original design and the main thing changed. |
| Room members inside room | **Referenced** (`room_members`) | Each member needs their own `lastReadAt` that changes on every read; updating one member must not rewrite or contend on the room document. Also lets us index "rooms of user X". |
| `lastMessage` inside room | **Embedded copy** (denormalised) | The chat list shows a preview for every room; reading it from `messages` would be one query per room. A small copy (id, sender name, 80-char preview, time) is updated on each send — eventual consistency is fine for a preview. |
| Reactions inside message | **Embedded** `Map<emoji, Set<userId>>` | Always read with the message, small and bounded in practice. |
| Mentions inside message | **Embedded** `Set<userId>` | Same reasoning. |
| Sender name in message | **Not stored**, loaded by id | Display names change; `MessageMapper` batch-loads users for a page (1 query). |

## 3. Collections in detail

### 3.1 `users`
```json
{
  "_id": "6abf7e2a6b10a52fd984703b",
  "username": "razi",                    // lowercase, unique
  "email": "razi@nexus.dev",             // lowercase, unique
  "passwordHash": "$2a$10$...",          // BCrypt, never returned by the API
  "displayName": "Razi Khan",
  "avatarColor": "#22d3ee",
  "about": "Full stack developer.",
  "customStatus": "Coding late",
  "lastSeenAt": "2026-10-02T09:49:52Z",
  "createdAt": "...", "updatedAt": "..."
}
```
| Query | Index |
|---|---|
| Login by username / email | `username` unique, `email` unique |
| Duplicate check on register | same unique indexes (also the real guard against a registration race) |
| Search people (`ContainingIgnoreCase`) | regex → **cannot use a normal index efficiently**; acceptable for a college-size collection, limited to 20 results. At scale: a text index or a search engine. |
| Load many users by id (mappers) | `_id` |

### 3.2 `rooms`
```json
{
  "_id": "6abf7e2e6b10a52fd984707d",
  "type": "DIRECT",                          // DIRECT | GROUP | CHANNEL
  "name": null, "description": null,
  "serverId": null,                          // set only for CHANNEL
  "directKey": "6abf..703b:6abf..703c",      // sorted user ids, only for DIRECT
  "iconColor": "#a78bfa", "createdBy": "...", "position": 0,
  "lastMessage": { "messageId": "...", "senderId": "...", "senderName": "Aisha Patel",
                   "preview": "Send me the link when it is up", "sentAt": "..." },
  "createdAt": "...", "lastActivityAt": "..."
}
```
**One collection for three kinds of conversation** keeps messages, membership, read receipts and real-time topics identical for DMs, groups and channels — one code path.

| Query | Index | Why |
|---|---|---|
| Channels of a server, ordered | `serverId` | Server sidebar |
| "Open DM with X" | `directKey` **unique, sparse** | Sparse: groups/channels have no key. Unique: two people can never get two DM rooms, even if both click at once |

### 3.3 `room_members`
```json
{ "_id": "...", "roomId": "...", "userId": "...", "role": "OWNER",
  "muted": false, "lastReadAt": "2026-10-02T10:22:44Z", "joinedAt": "..." }
```
| Query | Index | Why |
|---|---|---|
| Is user X in room R? (every send/read/subscribe) | `{roomId:1, userId:1}` **unique** | Fast permission check; uniqueness prevents double membership |
| Members of room R (fan-out unread, read ticks) | same index (prefix `roomId`) | |
| All rooms of user X (chat list, dashboard) | `userId` | `userId` is **not** the prefix of the compound index, so it needs its own |

`lastReadAt` powers **read receipts** (my message is "read" when every other member's `lastReadAt ≥ message.createdAt`) and is the **fallback for unread counts** when Redis is unavailable.

### 3.4 `messages`
```json
{
  "_id": "6abf85f6e34f128c8521a76c",
  "roomId": "6abf7e2e6b10a52fd984707d",
  "senderId": "6abf7e2a6b10a52fd984703c",
  "clientMessageId": "c-3f1d9a0e-...",      // idempotency key from the client
  "content": "Got it! Looks amazing @razi",
  "type": "TEXT",                            // TEXT | SYSTEM
  "replyToId": null,
  "mentions": ["6abf7e2a6b10a52fd984703b"],
  "reactions": { "🔥": ["6abf...703b", "6abf...703d"] },
  "edited": false, "deleted": false, "pinned": false,
  "createdAt": "2026-10-02T10:22:46.120Z", "editedAt": null
}
```
| Query | Index | Why |
|---|---|---|
| **Latest 30 messages of a room** | `{roomId:1, createdAt:-1}` | Equality on `roomId` + sort on `createdAt` → MongoDB walks the index from the newest entry and stops after 30. No collection scan, no in-memory sort, cost independent of room size |
| Older page (`before=<createdAt>`) | same index | Range on `createdAt` within the same `roomId` prefix |
| Idempotency lookup / guard | `{roomId:1, senderId:1, clientMessageId:1}` **unique, partial** (`clientMessageId` is a string) | Makes "same message twice" impossible at the database level. Partial → system messages (no key) are not indexed and never collide |
| Pinned messages of a room | prefix `roomId` of the main index + filter | Few pinned messages per room |
| Search in room | prefix `roomId` + regex on content | Bounded to one room, top 50 |
| Unread fallback count | prefix `roomId` + range `createdAt` | Only used when Redis is down |

**Soft delete:** `deleted=true`, content cleared. The row stays so replies pointing to it still render "This message was deleted".

### 3.5 `servers`, `server_members`, `invites`, `audit_logs`
```json
// servers
{ "_id": "...", "name": "Nexus HQ", "description": "...", "iconColor": "#22d3ee",
  "ownerId": "...", "discoverable": false, "createdAt": "..." }
// server_members
{ "_id": "...", "serverId": "...", "userId": "...", "role": "ADMIN", "nickname": null, "joinedAt": "..." }
// invites
{ "_id": "...", "code": "NEXUSHQ1", "serverId": "...", "createdBy": "...",
  "maxUses": 0, "uses": 3, "expiresAt": null, "createdAt": "..." }
// audit_logs
{ "_id": "...", "serverId": "...", "actorId": "...", "action": "MEMBER_KICKED",
  "targetId": "...", "details": "Zoya Shaikh", "createdAt": "..." }
```
| Collection | Index | Query |
|---|---|---|
| `server_members` | `{serverId, userId}` unique | permission checks, member list (prefix), no double join |
| `server_members` | `userId` | "my servers" |
| `invites` | `code` unique | resolve an invite link |
| `audit_logs` | `{serverId, createdAt:-1}` | newest-first log page |
| `servers` | — (discover list scans a small collection) | at scale: `{discoverable:1, createdAt:-1}` |

**Why server roles live in `server_members` and not in `users`:** a role is a property of the *relation* (Razi is OWNER of HQ but MEMBER elsewhere).
Channels also get `room_members` rows (created when a channel is created or a user joins) so read pointers and unread badges work exactly like DMs.

### 3.6 `friendships`
```json
{ "_id": "...", "requesterId": "...", "addresseeId": "...",
  "pairKey": "smallerId:largerId", "status": "PENDING", "createdAt": "...", "respondedAt": null }
```
`pairKey` unique → only one relation per pair regardless of who asked first. `requesterId` and `addresseeId` are each indexed because "my friends" is an `$or` query and MongoDB uses one index per `$or` branch.

### 3.7 `notifications`
```json
{ "_id": "...", "userId": "...", "type": "MENTION", "title": "Aisha mentioned you in #general",
  "body": "The new 3D landing page...", "link": "/app/servers/.../...", "actorId": "...",
  "read": false, "createdAt": "..." }
```
Index `{userId:1, createdAt:-1}` → newest-first inbox page without sorting in memory.

## 4. Pagination strategy

**Cursor (keyset) pagination for messages**, offset pagination for small lists.

```
GET /api/v1/rooms/{id}/messages?size=30                        → newest 30, hasNext
GET /api/v1/rooms/{id}/messages?size=30&before=<oldest createdAt> → the 30 before that
```
* **Why not `page=5&size=30` (skip/offset)?** `skip(150)` still walks 150 index entries, and if new messages arrive between requests the pages shift → duplicates or gaps while the user scrolls. A cursor anchored on a timestamp is stable and costs the same for page 1 and page 500.
* The query sorts newest-first (to use the index), the service reverses each page to oldest-first for display.
* `hasNext` comes from Spring Data `Slice` (fetches `size+1` internally) — no expensive `count()`.
* **Known trade-off:** two messages with the identical millisecond at a page boundary could be skipped. Fix if needed: cursor on `(createdAt, _id)`.
* Notifications and audit logs use `page/size` offsets: small, newest-first, rarely paged deep.

## 5. Consistency

* Single-document writes are atomic in MongoDB; every important invariant is either one document or a unique index (see ARCHITECTURE.md §6).
* Derived data (`room.lastMessage`, Redis unread counters, notification fan-out) is **eventually consistent** and recomputable from source data (`messages` + `room_members.lastReadAt`).
* No multi-document transactions: they require a replica set and would add latency to the hottest path (send) for data that can be rebuilt.

## 6. Scaling considerations

* **Shard key for `messages`:** `{roomId: "hashed"}` spreads writes; all hot queries include `roomId`, so they stay single-shard. Range sharding on `{roomId, createdAt}` would keep a room's history together but concentrate writes of hot rooms.
* **Big servers:** incrementing unread for 10 000 members per message is heavy; switch channels to "count since my `lastReadAt` on demand" (already the fallback code path).
* **Archival:** move messages older than N months to cold storage; the cursor API does not change.
* **Search:** move to a text index / Atlas Search / Elasticsearch fed by message events.
* **Read scaling:** replica set secondaries for history reads (with `readPreference=secondaryPreferred`) once write volume justifies it.
