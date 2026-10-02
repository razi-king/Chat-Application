# WebSocket Design

## 1. Why WebSocket (and why STOMP on top)

Chat needs the **server to push** data (a new message, "Aisha is typing", a read receipt) the moment it happens.

| Option | Verdict |
|---|---|
| Polling every N seconds | Simple, but either laggy (large N) or wasteful (small N); typing indicators are impractical |
| Long polling | Works, but complex to get right and heavy on threads |
| Server-Sent Events | Server → client only; sending still needs REST; fine for notifications, weak for typing |
| **WebSocket** | One persistent, two-way connection; low latency; supported everywhere |

Raw WebSocket is just a pipe of frames. **STOMP** adds destinations (`/topic/...`), subscriptions, per-user queues, headers and heartbeats —
and Spring supports it out of the box (`@EnableWebSocketMessageBroker`, `@MessageMapping`, `SimpMessagingTemplate`). That saved writing our own routing protocol.

## 2. Destinations

| Destination | Direction | Who | Carries (`ChatEvent.type`) |
|---|---|---|---|
| `/ws` | handshake | everyone | plain WebSocket endpoint (`/chat` = SockJS fallback) |
| `/app/rooms/{roomId}/send` | client → server | members | `SendMessageRequest {content, replyToId, clientMessageId}` |
| `/app/rooms/{roomId}/typing` | client → server | members | `TypingPayload {typing}` |
| `/topic/rooms/{roomId}` | server → clients | members (checked on SUBSCRIBE) | `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`, `TYPING`, `READ` |
| `/user/queue/events` | server → one user | that user | `UNREAD`, `NOTIFICATION`, `ROOM_UPDATED` |
| `/user/queue/errors` | server → one user | that user | `Response` error envelope (same shape as REST) |
| `/topic/presence` | server → clients | everyone | `PRESENCE {userId, online, lastSeenAt}` |

Every event uses one envelope (`dto/event/ChatEvent`): `{ type, roomId, payload, timestamp }`.

**Why per-user queues for unread/notifications instead of room topics?** A user is subscribed to only the room they are looking at, but the badge in the
sidebar must update for *all* rooms. Sending `UNREAD` to the user's private queue means one subscription per user instead of one per room.

## 3. Message flow — persistence and delivery are separate concerns

```
User A (client)                     Backend                                       User B
───────────────                     ───────                                       ──────
1. show optimistic bubble (⏱)
2. SEND /app/rooms/R/send ─────►  3. CONNECT was authenticated → Principal = A
                                   4. @Valid payload (else /user/queue/errors VAL_400)
                                   5. ChatService.sendMessage
                                        membership → idempotency → rate limit → validate reply
                                   6. PERSIST: messages.insert   ◄── point of truth
                                   7. DELIVER: /topic/rooms/R  MESSAGE_CREATED ──────────► 8. render bubble
                                   9. /user/queue/events UNREAD (count from Redis) ─────► 10. sidebar badge
11. echo arrives: replace the temp
    bubble (same clientMessageId), ✓
```

**Why separate?** Persistence must succeed or the request fails. Delivery is *best effort*: if nobody is subscribed, the message is still safe in MongoDB
and will be loaded with the history. Coupling them (e.g. "only save if delivered") would lose messages for offline users.

| Situation | What happens |
|---|---|
| **Receiver offline** | Nothing is lost. The message is in MongoDB; Redis unread counter was incremented. When B opens the app: chat list shows the badge (`GET /rooms`), opening the chat loads history (`GET /rooms/R/messages`) and marks it read |
| **Persistence fails** (MongoDB down) | Exception before any delivery → nobody sees a "ghost" message. REST caller gets `503 DB_503`; WebSocket caller gets the error on `/user/queue/errors`; the sender's bubble times out to "Not sent · retry". Retry uses the same `clientMessageId`, so a retry after a *partially* successful attempt cannot duplicate |
| **Delivery fails** (socket of B died mid-send) | The message is already stored. B's client reconnects and re-subscribes; the next history load or the chat list refresh shows it |
| **Sender's socket drops after the server saved but before the echo** | The bubble stays pending → after 8 s it offers retry → REST retry with the same `clientMessageId` → server finds the stored message and returns it (and re-broadcasts it) → no duplicate |
| **WebSocket disconnects** | `@stomp/stompjs` reconnects every 3 s; `SocketContext` re-subscribes every registered destination automatically; the UI shows "sync" instead of "live" in the server rail. While disconnected, sending falls back to REST |
| **Reconnecting** | On reconnect the server fires `SessionConnectedEvent` → presence back online. Messages that arrived while disconnected are fetched by the normal REST history/list calls the pages make |
| **Server restarts** | All sockets drop and reconnect; Redis presence counters are reset on startup (`PresenceServiceImpl.resetOnStartup`) because old counts are meaningless |
| **Malformed STOMP payload** | JSON conversion or `@Valid` fails → `@MessageExceptionHandler` → `Response` error on `/user/queue/errors` (`VAL_400` / `SYS_500`). The connection stays open |
| **Unauthorised SUBSCRIBE** | `ERROR` frame `ROOM_403`; no data is ever sent to that subscription |

## 4. Presence (online / offline)

* `SessionConnectedEvent` → `PresenceService.connected(userId)` → Redis `HINCRBY presence:connections userId 1`. Broadcast **online only when the count becomes 1**.
* `SessionDisconnectEvent` → decrement; at 0 → save `lastSeenAt` in MongoDB and broadcast offline.
* A user with 3 tabs is online until the last tab closes. Dead connections are detected by STOMP heartbeats (10 s) so the disconnect event eventually fires even if the browser crashed.

## 5. Typing and read receipts

* Typing: the composer sends `typing:true` at most every 2.5 s while typing and `typing:false` after 3 s idle or on send. Receivers expire a typing indicator after 5 s even if `false` never arrives (lost frame / closed tab).
* Read receipts: opening a room calls `POST /rooms/R/read` → `room_members.lastReadAt = now` → `READ {userId, lastReadAt}` to the room → the sender's ticks turn blue when every other member's `lastReadAt ≥ message.createdAt`.

## 6. Limits of the current design (and the upgrade path)

* The broker is **in memory** inside one JVM (`enableSimpleBroker`). Two backend instances would not see each other's subscribers.
* Upgrade: `enableStompBrokerRelay()` to RabbitMQ / ActiveMQ, or publish `ChatEvent`s through Redis pub/sub and let each instance forward to its local sockets. Only `RealtimeServiceImpl` and `WebSocketConfig` change — business services call the `RealtimeService` interface.
* Delivery is "at most once" over the socket; durability comes from MongoDB + history reload. True "delivered ✓✓ grey" receipts would need per-recipient acknowledgements.
