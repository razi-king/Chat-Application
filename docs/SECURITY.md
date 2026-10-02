# Security

## 1. Threat model (what we defend against)

| Threat | Defence |
|---|---|
| Password database leak | BCrypt hashing (`BCryptPasswordEncoder`, salted, slow by design); hashes never leave the server |
| Guessing passwords | Login rate limit: 10 attempts / minute / identifier (Redis `INCR`) |
| Discovering which usernames exist | Same `AUTH_402` "Invalid username or password" for unknown user and wrong password |
| Forged / tampered tokens | JWT signed with HMAC-SHA256; signature checked on every request |
| Stolen token after logout | Token id (`jti`) blacklisted in Redis until its natural expiry |
| Impersonation ("send as Aisha") | Sender / actor is **always** the id from the token; DTOs have no `senderId` field |
| Reading other people's chats | `AccessGuard.requireRoomMember` on every room operation **and** STOMP `SUBSCRIBE` check in `WebSocketAuthInterceptor` |
| Privilege escalation in servers | Role ranks (`OWNER > ADMIN > MEMBER`), `requireServerRole`, "can only act on lower roles", owner role not assignable |
| Quoting a message from another room | `replyToId` must belong to the same room |
| Malformed / huge input | Bean Validation on every DTO (sizes, patterns), 2000-char messages, `HttpMessageNotReadableException` → 400 |
| Spam / flooding | Message rate limit (8 per 5 s per user) |
| Internal details leaking | Global handler returns generic messages for 500/503; stack traces only in logs |
| Sensitive data in logs | Access log prints method + path + status only (no query strings, no headers, no bodies); tokens/passwords are never logged |
| Log injection via headers | Client-provided `X-Request-Id` accepted only if it matches `^[a-zA-Z0-9-]{8,64}$` |
| Cross-origin abuse | CORS allow-list (`app.cors.allowed-origins`), same list for WebSocket origins |
| CSRF | Not applicable: no cookies are used for auth; the token is sent explicitly in a header, so a foreign site cannot make the browser attach it. CSRF protection is therefore disabled deliberately |

## 2. From login to an authenticated request

```
1. POST /auth/login {identifier, password}
2. AuthServiceImpl: rate limit → find user → BCrypt.matches
3. JwtService.generateToken: header {alg: HS256}
                             payload {jti: uuid, sub: userId, username, iat, exp: now+72h}
                             signature = HMAC_SHA256(secret, header.payload)
4. Client stores the token (localStorage) and sends "Authorization: Bearer <token>"
5. Every request: JwtAuthenticationFilter
     parse → verify signature (forged? → AUTH_401) → verify exp (expired? → AUTH_403)
     → jti blacklisted? (logged out → AUTH_403)
     → SecurityContext.setAuthentication(AuthUser(id, username))
6. SecurityConfig decides: permitAll (register, login, invite preview, health, swagger, /ws handshake) or authenticated
7. Controller receives @AuthenticationPrincipal AuthUser → service → AccessGuard checks resource-level permissions
```

**Authentication** = "who are you?" (steps 1–6, JWT). **Authorization** = "may you do this to *this* resource?" (step 7, `AccessGuard` + role checks).
There are no global admin roles: permissions are per server / per group, which matches how chat products work.

### 2.1 Role-based access control (RBAC)

Roles are **scoped to a server or a group**, not global: Razi can be OWNER of "Nexus HQ" and a plain MEMBER of another server.
Server roles live in `server_members.role`; group roles in `room_members.role`. Ranks are ordered (`MemberRole`: OWNER 3 > ADMIN 2 > MEMBER 1)
and checked with `role.atLeast(...)` inside `AccessGuard` — the frontend only hides buttons, **the backend enforces every rule**.

**Servers**

| Action | MEMBER | ADMIN | OWNER | Code |
|---|:-:|:-:|:-:|---|
| Read / send messages, react, reply, create invite | ✅ | ✅ | ✅ | `requireRoomMember`, `requireServerMember` |
| Edit / delete **own** message | ✅ | ✅ | ✅ | sender check |
| Delete **others'** messages, pin in channels | ❌ | ✅ | ✅ | `canModerate` |
| Create / delete channels, edit server, list invites, view audit log | ❌ | ✅ | ✅ | `requireServerRole(ADMIN)` |
| Kick a member | ❌ | ✅ (MEMBERs only) | ✅ (MEMBERs + ADMINs) | target role must be **lower** than yours |
| Promote / demote (MEMBER ⇄ ADMIN) | ❌ | ❌ | ✅ | `requireServerRole(OWNER)`; OWNER role can't be given or taken |
| Delete server | ❌ | ❌ | ✅ | `requireServerRole(OWNER)` |
| Leave server | ✅ | ✅ | ❌ (delete instead) | `OWNER_CANNOT_LEAVE` |

**Groups (WhatsApp side):** the creator is OWNER and everyone else is MEMBER. Admins (OWNER here) can add and remove lower-ranked members, only the OWNER can delete the group, and if the OWNER leaves, the longest-standing member becomes OWNER. **DMs** have no roles: both people are equal members.

Denied actions return `403` with `AUTH_404` / `ROOM_403` / `SRV_403`. Demo data: in Nexus HQ, `razi` = OWNER, `aisha` = ADMIN, `kabir`/`zoya` = MEMBER.

## 3. Why JWT (and its trade-offs)

* **Stateless:** any backend instance can verify a token with the shared secret — no session store needed to scale horizontally.
* **Works for WebSocket:** the same token is sent once in the STOMP `CONNECT` frame.
* **Trade-off — revocation:** a JWT is valid until `exp`. We add a small Redis blacklist of revoked `jti`s (only logged-out tokens, each with a TTL = remaining lifetime), so the check stays O(1) and the list never grows unbounded.
* **Trade-off — storage:** `localStorage` is readable by JavaScript, so an XSS bug could steal the token. Mitigations: React escapes all rendered text (messages are never injected as HTML), no `dangerouslySetInnerHTML`. Production upgrade: short-lived access token in memory + refresh token in an `HttpOnly; Secure; SameSite` cookie.
* **Secret management:** `app.jwt.secret` comes from `JWT_SECRET`; the default value is for development only and must be replaced (≥ 32 random bytes).

## 4. WebSocket security

The HTTP upgrade to `/ws` is public (browsers cannot attach custom headers to a WebSocket handshake). Authentication happens one step later:

| STOMP frame | Check in `WebSocketAuthInterceptor` | Failure |
|---|---|---|
| `CONNECT` | `Authorization` native header → `JwtService.parse` → blacklist check → `accessor.setUser(AuthUser)` | `ERROR` frame `AUTH_401/AUTH_403`, connection closed; the client logs the user out |
| `SUBSCRIBE /topic/rooms/{id}` | user must be in `room_members` | `ERROR` frame `ROOM_403` |
| `SUBSCRIBE /user/queue/...` | Spring routes per-user queues by the authenticated principal, so a user can only receive their own queue | — |
| `SEND /app/...` | handled by `@MessageMapping` with the authenticated `Principal`; the service re-checks membership | error sent privately to `/user/queue/errors` |

## 5. Data exposure rules

* `UserResponse.email` is filled **only** by `UserMapper.toSelf` (your own profile).
* `passwordHash` exists only in the entity; there is no DTO field for it.
* Deleted messages return empty `content`.
* Error responses for 500/503 never include exception messages (tested in `RestResponseExceptionHandlerTest`).

## 6. Production checklist

* [ ] Set `JWT_SECRET` (random, ≥ 32 bytes) and keep it out of git
* [ ] Serve over HTTPS / WSS only; set `CORS_ORIGINS` to the real domain
* [ ] Set `SEED_DEMO_DATA=false`
* [ ] Enable MongoDB authentication and use a least-privilege DB user; enable Redis `requirepass` / ACLs
* [ ] Consider refresh tokens + HttpOnly cookies, account lockout notifications, and a Content-Security-Policy header
