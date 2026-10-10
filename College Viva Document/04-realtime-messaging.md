# 4. Real-time messaging

## Why WebSocket?
Chat needs the **server to push** data the moment it happens. With polling (asking every few seconds) the app is either slow or wastes requests. A WebSocket keeps **one open two-way connection**.

**STOMP** is a small protocol on top of WebSocket. It adds destinations ("channels"), subscriptions and private per-user queues, and Spring supports it out of the box.

| Destination | Direction | Carries |
|---|---|---|
| `/app/rooms/{roomId}/send` | browser -> server | A new message |
| `/app/rooms/{roomId}/typing` | browser -> server | "I am typing" |
| `/topic/rooms/{roomId}` | server -> room members | New/edited/deleted messages, typing, read ticks |
| `/user/queue/events` | server -> one user | Unread counts, notifications, room changes |
| `/user/queue/errors` | server -> one user | Errors in the same JSON format as REST |
| `/topic/presence` | server -> everyone | Online / offline |

## Sending a message, step by step
1. **Send**: the bubble appears instantly with a clock icon and a unique **clientMessageId** (optimistic UI).
2. **Verify**: the WebSocket was authenticated with the JWT when it connected.
3. **Check** (`ChatServiceImpl.sendMessage`):
   - Is the sender a member of the room? If not: 403.
   - Was this clientMessageId already saved? If yes: return the saved one (**no duplicate**).
   - Rate limit: max 8 messages per 5 seconds.
   - A reply must point to a message in the same room.
4. **Save** in MongoDB. From now on the message is safe.
5. **Deliver**: pushed to everyone subscribed to `/topic/rooms/{roomId}`.
6. **Notify**: Redis unread +1 for every other member; @mentions create notifications.
7. The sender's bubble is replaced by the saved message and shows a tick. When the receiver opens the chat the ticks turn **blue**.

**Save first, deliver second.** Saving must succeed or the request fails. Delivery is best effort: if nobody is listening, the message is still in MongoDB.

## What if ...?

| Situation | What happens |
|---|---|
| Receiver is offline | Nothing is lost. The message is in MongoDB and the unread counter went up. When they come back they see the badge and the history. |
| WebSocket disconnects | The browser reconnects every 3 seconds and re-subscribes automatically. Meanwhile sending falls back to REST. |
| Message never confirmed | After 8 seconds the bubble shows "Not sent, retry". The retry uses the **same** clientMessageId. |
| MongoDB is down | The save fails before any delivery, so nobody sees a message that was never stored. The user gets 503 DB_503. |
| Same message sent twice (retry) | The unique index `{roomId, senderId, clientMessageId}` lets only one copy in; the other request gets the stored one back. |
| Server restarts | Everyone reconnects automatically; presence counters are reset. |

## No duplicate messages (idempotency)
**Idempotency** = doing something twice has the same effect as doing it once.
- The client creates a `clientMessageId` (UUID) once per message and reuses it on every retry.
- MongoDB has a **unique index** on `{roomId, senderId, clientMessageId}`.
- If two copies race, the database accepts exactly one; the other gets `DuplicateKeyException` and returns the winner.
- Proven by a test: **10 parallel retries -> 1 stored message**.

## Read receipts, typing and presence
- **Read ticks:** opening a chat sets `room_members.lastReadAt = now` and broadcasts a READ event. A message is blue when every other member's `lastReadAt` is after it.
- **Typing:** sent at most every 2.5 seconds while typing; nothing is saved; hidden after 5 seconds.
- **Online status:** Redis counts open connections per user. Online when the count goes 0 -> 1, offline when it goes back to 0 (so 3 open tabs = still online after closing one). `lastSeenAt` is saved in MongoDB.
