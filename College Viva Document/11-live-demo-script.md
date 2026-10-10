# 11. Live demo script: the app, MongoDB Compass and Redis side by side

Follow these steps in order. For each one: **do this in the app**, **then show this in Compass / Redis**, **then say this**.

## 0. Setup (before the professor arrives)
| Window | Open |
|---|---|
| Laptop browser | The Nexus website (Vercel link), logged in as **razi** |
| Professor's phone (or a private window) | The same link, log in as **aisha** |
| MongoDB Compass | Your **Atlas** connection → database **`chatapp`** |
| Upstash console | Your Redis database → **Data Browser** tab |

Open the website 2–3 minutes early: the free server needs about a minute to wake up.

**Compass basics you will use in every step:**
- Click a collection on the left → documents appear.
- **Refresh** (circular arrow) after every action in the app; Compass does not update by itself.
- **Sort** box: `{ createdAt: -1 }` puts the newest document first.
- **Filter** box: for example `{ type: "GROUP" }`.
- Ids: a document's own id is `_id: ObjectId("...")`. When one document points to another (like `roomId`, `userId`), it is stored as a **plain string**, so filter with quotes: `{ roomId: "6abf7e2e..." }`.

**Opening line:** *"The app has 10 collections. I will do actions in the app and show exactly which documents are created or changed."*

---

## 1. Users: register and login
**Do:** show the login page. Optionally register a new user live.

**Show `users`:** a document has `username`, `email`, `displayName`, `avatarColor`, `lastSeenAt`, and `passwordHash: "$2a$10$..."`.

**Say:** *"Passwords are never stored. This is a BCrypt hash. On login the server compares hashes and returns a JWT token."*

**Redis:** a `rate:login:<username>:<window>` key appears for 60 seconds. *"Max 10 login attempts per minute, to stop password guessing."*

**Show the lockout:** log out, type a wrong password 5 times. The 5th answer says *"Too many failed login attempts. Account locked, try again in 15 minutes"*, and even the correct password is refused. **Redis:** `auth:lock:<user id>` with a 15-minute TTL (the `auth:fail:` counter counted 1 to 5). *"Locking the account stops slow guessing too, not just fast floods."*

---

## 2. Online status (Redis presence)
**Do:** razi is logged in on the laptop; aisha logs in on the phone.

**Show Redis → `presence:connections`:** a hash like `razi-id → 1`, `aisha-id → 1`. Open a second tab as razi → `2`. Close it → `1`.

**Show in the app:** the green online dot next to aisha. When aisha closes the app, the dot turns grey and "last seen" appears.

**Then show `users` → aisha → `lastSeenAt`** got updated.

**Say:** *"Redis counts open connections per user. Online when it goes 0 → 1, offline only when it reaches 0, so several tabs don't flicker. When the user goes offline, lastSeenAt is saved permanently in MongoDB."*

---

## 3. Friends and notifications
**Do (razi):** Home → Friends → **Add friend** → type `zoya` (or the user you registered) → Send.

**Show `friendships`:** a new document:
```
requesterId: "<razi id>", addresseeId: "<zoya id>",
pairKey: "<smaller id>:<bigger id>", status: "PENDING"
```
**Show `notifications`** (sort `{ createdAt: -1 }`): `type: "FRIEND_REQUEST"`, `userId: <zoya id>`, `read: false`.

**Do (log in as zoya, or have it open):** the bell icon shows a badge and a pop-up appears instantly. Accept the request.

**Refresh `friendships`:** `status: "ACCEPTED"` and `respondedAt` filled. **`notifications`:** a new `FRIEND_ACCEPTED` for razi.

**Say:** *"pairKey is unique, so two people can only ever have one friendship document, even if both send requests at the same time. The notification is saved in MongoDB and also pushed live over the WebSocket."*

---

## 4. One-to-one chat (DM), and the ticks
**Do (razi):** Chats → open **Aisha Patel** (or start a new chat with the search icon).

**Show `rooms`** with filter `{ type: "DIRECT" }`: `directKey: "<id1>:<id2>"` (unique, so only one DM per pair), and `lastMessage { preview, senderName, sentAt }`.

**Show `room_members`** with filter `{ roomId: "<that room's _id as a string>" }`: **2 documents**, one for razi and one for aisha, each with **`lastReadAt`**.

### Sending a message and the ticks

| Bubble shows | Meaning | What happened in the database |
|---|---|---|
| Clock icon | Sending | Not in MongoDB yet |
| **Single grey tick** ✓ | **Saved on the server** | Document inserted in `messages` |
| **Double grey ticks** ✓✓ (groups only) | **Read by some** members | Some members' `lastReadAt` ≥ message time |
| **Double blue ticks** ✓✓ | **Read by everyone** | Every other member's `lastReadAt` ≥ message time |
| "Not sent, retry" | Not confirmed in 8 s | Retry uses the same `clientMessageId`, so no duplicate |

**Do (razi):** close the chat on aisha's phone first, then send **"Hello professor"**.

**Show `messages`** (sort `{ createdAt: -1 }`): the new document at the top: `roomId`, `senderId`, `content`, `clientMessageId`, `createdAt`. The bubble shows a **single grey tick**.

**Show Redis → `unread:<aisha id>`:** `<roomId> → 1`. Aisha's chat list shows a badge **1**.

**Do (aisha):** open the chat on the phone.

**Refresh `room_members`:** aisha's `lastReadAt` jumped to now. **Redis:** the `unread:<aisha id>` entry for that room is gone. **On razi's laptop:** the tick turned **double blue**, live.

**Say:** *"Ticks are not stored inside each message. Each member has a lastReadAt in room_members. A message is read when the other person's lastReadAt is later than the message's createdAt. Opening the chat updates lastReadAt and pushes a READ event, so the ticks turn blue instantly."*

**Bonus, typing indicator:** aisha types without sending → razi sees "Aisha is typing…". *"Typing is sent over the WebSocket only. Nothing is saved."*

**Bonus, reply / react / edit / delete:** react with 🔥 → `messages.reactions: { "🔥": ["<razi id>"] }`. Edit → `edited: true`. Delete → `deleted: true` with the content cleared (soft delete: the document stays so replies still work).

---

## 5. Group chat
**Do (razi):** Chats → **New group** icon → tick 2 friends → name it "Viva Demo" → Create.

**Show `rooms`** (filter `{ type: "GROUP" }`): the new room with `name: "Viva Demo"`, `createdBy`.

**Show `room_members`** (filter `{ roomId: "<group id>" }`): razi has `role: "OWNER"`, the others `role: "MEMBER"`.

**Show `messages`:** a `type: "SYSTEM"` message "Razi Khan created the group…".

**Show `notifications`:** an `ADDED_TO_GROUP` notification for each member.

**Do:** send a message with **`@aisha`** in it.
**Show `messages`:** `mentions: ["<aisha id>"]`. **`notifications`:** a `MENTION` for aisha (she gets a live pop-up).
**Ticks in a group:** grey ✓✓ when **some** members have read it, blue ✓✓ when **all** have.

**Say:** *"Groups use the same rooms, room_members and messages collections as DMs. Only type changes. The creator is OWNER and can add or remove members; if the owner leaves, the oldest member becomes owner."*

---

## 6. Communities: servers and channels (Discord side)
**Do (razi):** click **+** (or the Servers tab on a phone) → **Server** → name "Data Analytics Class" → Create.

**One action, many collections.** Refresh each:

| Collection | What appears |
|---|---|
| `servers` | The server: `name`, `ownerId`, `discoverable` |
| `server_members` | razi with `role: "OWNER"` |
| `rooms` (filter `{ type: "CHANNEL" }`) | 3 channels: `general`, `announcements`, `off-topic`, each with `serverId` |
| `room_members` | razi in each channel (for unread and read tracking) |
| `messages` | SYSTEM welcome message in #general |
| `audit_logs` | `action: "SERVER_CREATED"` |

**Do:** server menu (top of the channel list) → **Invite people** → copy the link → open it on aisha's phone → **Accept invite**.

| Collection | What changes |
|---|---|
| `invites` | The `code`, `expiresAt`, and **`uses`** goes up by 1 |
| `server_members` | aisha added with `role: "MEMBER"` |
| `room_members` | aisha added to every channel |
| `messages` | SYSTEM "Aisha Patel joined the server. Say hi!" |
| `notifications` | `SERVER_JOINED` for razi (the owner) |
| `audit_logs` | `MEMBER_JOINED` |

### Roles (RBAC)
**Do (razi, Owner):** open the member list → click aisha → **Make admin**.
**Show `server_members`:** aisha's `role` changed `MEMBER → ADMIN`. **`audit_logs`:** `ROLE_CHANGED`.

**Do:** log in as a plain **MEMBER** (for example kabir in Nexus HQ): there is no "Create channel" button. Even calling the API directly returns **403 Forbidden**.

**Do (Owner or Admin):** server menu → **Audit log** → the same entries appear in the app.

**Say:** *"Roles live in server_members because a role belongs to the user-and-server pair. Owner > Admin > Member, and you can only act on someone below you. AccessGuard checks this in the backend, so hiding a button is not the only protection. Every admin action is written to audit_logs."*

---

## 7. Redis summary (show the Upstash Data Browser)

| Key | What you see | Changes when |
|---|---|---|
| `presence:connections` | userId → open connections | Login/logout, opening/closing tabs |
| `unread:<userId>` | roomId → unread count | +1 when someone else sends; removed when the chat is opened |
| `rate:message:<userId>:<window>` | a small number | Each message sent; disappears after 5 s |
| `rate:login:<name>:<window>` | a small number | Each login attempt; disappears after 60 s |
| `auth:fail:<userId>` / `auth:lock:<userId>` | failed-attempt count / "1" with TTL | Wrong passwords; after 5 the account is locked for 15 min |
| `auth:blacklist:<tokenId>` | "1" with a countdown (TTL) | On logout; deletes itself when the token would expire |

**Say:** *"MongoDB is permanent memory, Redis is fast short-term memory for counters that change on almost every action. Redis commands like HINCRBY are atomic, so two messages at the same moment never lose a count. If Redis goes down, the app falls back to MongoDB and keeps working."*

---

## 8. Finish with indexes (shows database understanding)
**Compass → `messages` → Indexes tab:** `room_created {roomId: 1, createdAt: -1}` and `idempotency_key` (UNIQUE, PARTIAL).

**Explain Plan tab:** Filter `{ roomId: "<a room id>" }`, Sort `{ createdAt: -1 }`, Limit 30 → **Explain** → shows **IXSCAN**, and documents examined ≈ 30.

**Say:** *"Without this index MongoDB would read every message of every chat. With it, it jumps straight to this chat's newest 30 messages, the same speed for 100 or 10 million messages. The second unique index is why a retried message can never be stored twice."*

---

## Quick map: action → collections

| Action in the app | Collections written | Redis |
|---|---|---|
| Register | users | – |
| Login | – | rate:login |
| Open app (socket connects) | – | presence:connections +1 |
| Close app | users.lastSeenAt | presence:connections −1 |
| Friend request / accept | friendships, notifications | – |
| Open a new DM | rooms (DIRECT), room_members ×2 | – |
| Send message | messages, rooms.lastMessage, room_members (sender lastReadAt), notifications (if @mention) | unread +1 for others, rate:message |
| Open a chat | room_members.lastReadAt | unread entry removed |
| React / edit / delete / pin | messages (+ audit_logs for pin in a channel) | – |
| Create group | rooms (GROUP), room_members, messages (SYSTEM), notifications | – |
| Create server | servers, server_members, rooms (CHANNEL ×3), room_members, messages, audit_logs | – |
| Create / accept invite | invites, server_members, room_members, messages, notifications, audit_logs | – |
| Change role / kick | server_members (+ room_members on kick), audit_logs | – |
| Logout | – | auth:blacklist |
