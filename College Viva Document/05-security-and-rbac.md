# 5. Security and RBAC

## Passwords
Passwords are hashed with **BCrypt** (salted and deliberately slow). The database never contains real passwords, and the hash is never sent to the browser.

## JWT login
**JWT** (JSON Web Token) = `header.payload.signature`.
- Payload: user id, username, a unique token id (`jti`), expiry (72 hours).
- Signature: made with a secret key (`JWT_SECRET`), so nobody can change the token without the server noticing.
- A JWT is **signed, not encrypted**: anyone can read the payload, so it contains no secrets.

Flow:
1. Login succeeds -> server returns the token.
2. The browser sends `Authorization: Bearer <token>` with every request.
3. `JwtAuthenticationFilter` checks the signature, the expiry and the logout list.
4. Valid -> the user is known for this request. Invalid -> 401 with code `AUTH_401` or `AUTH_403` (expired).

**Why JWT?** The server stores no sessions, so any server instance can check a token, and the same token works for the WebSocket.

**Logout:** a JWT stays valid until it expires, so on logout its `jti` is saved in Redis (`auth:blacklist:<id>`) until that time. The filter rejects it from then on.

## Authentication vs authorization
- **Authentication** = who are you? (login, JWT)
- **Authorization** = what are you allowed to do? (roles, membership)

## WebSocket security
Browsers cannot add headers to the WebSocket handshake, so:
- the token is sent in the STOMP **CONNECT** frame and checked by `WebSocketAuthInterceptor`;
- every **SUBSCRIBE** to `/topic/rooms/{id}` is checked: only room members can listen.

## RBAC: roles per server
Roles are stored in `server_members.role`, because a role belongs to the **relation** between a user and a server (Razi can be Owner of one server and Member of another). Ranks: **Owner (3) > Admin (2) > Member (1)**.

| Action | Member | Admin | Owner |
|---|---|---|---|
| Chat, react, reply, create invite | Yes | Yes | Yes |
| Edit / delete own messages | Yes | Yes | Yes |
| Delete others' messages, pin in channels | No | Yes | Yes |
| Create / delete channels, edit server, see audit log | No | Yes | Yes |
| Kick someone | No | Members only | Members and Admins |
| Promote / demote (Member <-> Admin) | No | No | Yes |
| Delete server | No | No | Yes |
| Leave server | Yes | Yes | No (must delete) |

**Rule:** you can only act on someone **below** your rank.

**Groups:** the creator is Owner, others are Members; if the Owner leaves, the oldest member becomes Owner. **DMs:** no roles.

**Where it is enforced:** `AccessGuard` in the backend (`requireServerRole(serverId, userId, ADMIN)`). The frontend only hides buttons; calling the API directly as a Member still returns **403 Forbidden**.

**Demo data:** in "Nexus HQ", `razi` = Owner, `aisha` = Admin, `kabir` and `zoya` = Members.

## Other protections

| Threat | Protection |
|---|---|
| Password guessing | Max 10 login attempts per minute, and **5 wrong passwords lock the account for 15 minutes** (Redis keys `auth:fail:<account>` and `auth:lock:<account>`; works in memory if Redis is down) |
| Finding valid usernames | Same error for wrong user and wrong password |
| Pretending to be someone else | Sender always taken from the token, never from the request |
| Spam | Max 8 messages per 5 seconds |
| Bad input | Validation on every DTO (lengths, formats) |
| Leaking internals | 500/503 errors return generic messages; details only in logs |
| Secrets in logs | Logs show method, path, status, request id; never tokens or passwords |
| Other websites calling the API | CORS allows only the real frontend address |
