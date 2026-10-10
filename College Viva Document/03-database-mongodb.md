# 3. Database: MongoDB

## Why MongoDB?
- Chat data is naturally a **document**: a message with its reactions and mentions inside it.
- Messages are mostly **added**, and almost always read **by chat, newest first**, which fits one compound index perfectly.
- The flexible schema let me add reactions, pins and mentions without migrations.
- Trade-off: no joins and limited transactions, so rules are enforced with **single-document writes** and **unique indexes**.

## The 10 collections

| Collection | Stores | Important fields |
|---|---|---|
| `users` | Accounts and profiles | username (unique), email (unique), passwordHash (BCrypt), lastSeenAt |
| `rooms` | Every conversation: DM, group or channel | type (DIRECT/GROUP/CHANNEL), serverId, directKey, lastMessage |
| `room_members` | Who is in which room | roomId, userId, role, **lastReadAt** (read ticks) |
| `messages` | Every message | roomId, senderId, content, reactions, replyToId, **clientMessageId**, createdAt |
| `servers` | Discord-style communities | name, ownerId, discoverable |
| `server_members` | Who is in which server and their role | serverId, userId, role (OWNER/ADMIN/MEMBER) |
| `invites` | Invite codes | code (unique), uses, maxUses, expiresAt |
| `friendships` | Friend requests | requesterId, addresseeId, pairKey (unique), status |
| `notifications` | Notification inbox | userId, type (MENTION, FRIEND_REQUEST ...), read |
| `audit_logs` | Admin actions in a server | serverId, actorId, action (MEMBER_KICKED ...) |

## The most important design decision
The original project stored **all messages inside the room document**. That was changed because:
1. A MongoDB document can be at most **16 MB**; a busy chat would break.
2. Every new message rewrote the whole room, so two people sending at once could **overwrite** each other.
3. Paging through an array inside one document is awkward.

Now every message is **its own document** in `messages`.

**Embedded vs referenced (how I decided):** embed when the data is small and always read together (reactions, mentions, the `lastMessage` preview); reference when it grows without limit or changes on its own (messages, members).

## Indexes (why queries stay fast)

| Collection | Index | Used for |
|---|---|---|
| messages | `{roomId: 1, createdAt: -1}` | Latest 30 messages of a chat, older pages |
| messages | `{roomId, senderId, clientMessageId}` unique (partial) | Blocking duplicate messages |
| room_members | `{roomId, userId}` unique + `userId` | Permission check; my chat list |
| server_members | `{serverId, userId}` unique + `userId` | Permission check; my servers |
| rooms | `directKey` unique (sparse), `serverId` | One DM per pair; a server's channels |
| users | `username` unique, `email` unique | Login, no duplicate accounts |
| friendships | `pairKey` unique, `requesterId`, `addresseeId` | One relation per pair; my friends |
| notifications | `{userId, createdAt: -1}` | Newest notifications first |
| audit_logs | `{serverId, createdAt: -1}` | Newest audit entries first |
| invites | `code` unique | Opening an invite link |

**Without an index** MongoDB reads every document (a "collection scan"), so the app slows down as data grows. **With** `{roomId, createdAt}` it jumps straight to one chat's newest messages and reads only 30. You can prove this in MongoDB Compass: Explain Plan shows **IXSCAN**.

## Pagination
Chat history uses **cursor pagination**:
```
GET /api/v1/rooms/{id}/messages?size=30                      -> newest 30
GET /api/v1/rooms/{id}/messages?size=30&before=<createdAt>   -> the 30 before that
```
Why not `page=5`? Offset paging gets slower the deeper you go, and new messages arriving while you scroll would shift the pages (duplicates or gaps). A cursor is stable and always fast.

## Consistency (no transactions needed)
- The message insert is **one document**, so it is atomic.
- Rules that must never break are **unique indexes** (one DM per pair, one membership, one message per clientMessageId).
- Derived data (the chat preview, unread counts) may be briefly out of date and can always be recomputed. This is called **eventual consistency**.
- MongoDB transactions need a replica set and add delay to the busiest operation, so they are not used.

## Sample message document
```json
{
  "_id": "6abf85f6e34f128c8521a76c",
  "roomId": "6abf7e2e6b10a52fd984707d",
  "senderId": "6abf7e2a6b10a52fd984703c",
  "clientMessageId": "c-3f1d9a0e-8c2b",
  "content": "Got it! Looks amazing @razi",
  "type": "TEXT",
  "mentions": ["6abf7e2a6b10a52fd984703b"],
  "reactions": { "🔥": ["6abf7e2a6b10a52fd984703b"] },
  "edited": false, "deleted": false, "pinned": false,
  "createdAt": "2026-10-02T10:22:46.120Z"
}
```
