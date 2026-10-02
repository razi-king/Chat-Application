# Error Handling

## 1. Strategy in one sentence

**Business code throws typed exceptions; exactly one place per transport turns them into the standard `Response` envelope; the frontend turns every failure into one `ApiError` type and handles it in one function.**
No `try/catch` is scattered through services to build error responses.

## 2. Backend exception hierarchy

```
RuntimeException
 └── ChatAppException (errorCode: ErrorCode, meta: Object)     ← base: carries everything the handler needs
      ├── ResourceNotFoundException     404-type codes (ROOM_404, USER_404, ...)
      ├── DuplicateResourceException    409-type codes (USER_409, ROOM_409, FRD_409) + optional field meta
      ├── UnauthorizedException         401 codes (AUTH_401/402/403)
      ├── ForbiddenException            403 codes (ROOM_403, SRV_403, MSG_403, AUTH_404)
      ├── BadRequestException           400 business-rule violations (SRV_410, FRD_400, ...)
      └── RateLimitException            429 (RATE_429)
```

**Why this shape (and not 50 classes)?**
* The **HTTP status lives in `ErrorCode`**, not in the class. `ROOM_404` and `USER_404` share one class because the handler treats them identically. A new error is one enum line (Open/Closed principle).
* The six subclasses exist for **readability at the throw site** (`throw new ForbiddenException(ErrorCode.NOT_ROOM_MEMBER)` reads like the rule) and for tests (`isInstanceOf(ForbiddenException.class)`).
* They are unchecked: callers cannot do anything useful with them except let them bubble up, so forcing `throws` declarations would just add noise.
* `meta` carries field-level details, e.g. `{"username": "Username is already taken"}` so the form highlights that input.

## 3. `ErrorCode` (excerpt)

| Code | HTTP | Meaning |
|---|---|---|
| `VAL_400` | 400 | DTO validation failed (fields in `errorMeta`) |
| `REQ_400` / `REQ_404` / `REQ_409` | 400 / 404 / 409 | malformed request / unknown endpoint / uncaught duplicate key |
| `AUTH_401` / `AUTH_402` / `AUTH_403` / `AUTH_404` | 401 / 401 / 401 / 403 | not logged in / bad credentials / token expired or revoked / no permission |
| `USER_404`, `USER_409`, `USER_410` | 404 / 409 / 409 | user not found / username taken / email taken |
| `ROOM_404`, `ROOM_403`, `ROOM_409` | | room not found / not a member / channel name used |
| `SRV_404`, `SRV_403`, `SRV_410` | | server not found / not a member / owner cannot leave |
| `MSG_404`, `MSG_403` | | message not found / not your message |
| `INV_404`, `INV_410` | | invite invalid / expired |
| `RATE_429` | 429 | too many requests |
| `DB_503` | 503 | database unavailable — retry later |
| `SYS_500` | 500 | unexpected bug |

## 4. Where errors are translated

| Source | Translator | Output |
|---|---|---|
| Any `ChatAppException` in a REST call | `RestResponseExceptionHandler.handleChatAppException` | status from `ErrorCode` |
| `@Valid` failure | `handleValidation` | 400 `VAL_400` + `errorMeta` per field |
| Unreadable JSON / wrong param type | `handleBadBody` | 400 `REQ_400` |
| `DuplicateKeyException` not handled by a service | `handleDuplicateKey` | 409 `REQ_409` |
| MongoDB down / timeout (`DataAccessResourceFailureException`, `TransientDataAccessException`) | `handleDatabaseUnavailable` | 503 `DB_503` |
| Anything else | `handleUnexpected` | 500 `SYS_500`, full stack trace **only in the log** |
| Missing/invalid/expired token (before controllers) | `SecurityErrorHandlers.commence` | 401 with the precise code from `JwtAuthenticationFilter` |
| Access denied by Spring Security | `SecurityErrorHandlers.handle` | 403 `AUTH_404` |
| Error inside a STOMP `@MessageMapping` | `ChatController` `@MessageExceptionHandler`s | same `Response` envelope sent to `/user/queue/errors` |
| Bad token on STOMP CONNECT / forbidden SUBSCRIBE | `WebSocketAuthInterceptor` | STOMP `ERROR` frame `"AUTH_403: ..."` / `"ROOM_403: ..."` |

Every error body includes `requestId` (from `RequestIdFilter`) so a user report can be matched to log lines.

## 5. Where `try/catch` IS used (deliberately)

| Place | Why catching here is correct |
|---|---|
| `ChatServiceImpl.sendMessage` catches `DuplicateKeyException` | It is not an error: it means "a concurrent copy won" → return that message (idempotency) |
| `RoomServiceImpl.openDirect` catches `DuplicateKeyException` | Two users opened the same DM simultaneously → return the existing room |
| `JwtService.parse` catches jjwt exceptions | Translate library exceptions into our domain codes (expired vs invalid) |
| `RedisSafe.call` catches `RuntimeException` | Redis is an optional dependency; failure → fallback + one warning log (never silently empty) |
| `JwtAuthenticationFilter` catches `ChatAppException` | A bad token must not crash the filter chain; it records why, and the entry point answers 401 |

## 6. Frontend side

```
axios error ──► interceptor (services/api.ts)
                 no response      → ApiError(NET_000)  "Is the backend running?"
                 timeout          → ApiError(NET_408)
                 HTTP error       → ApiError.fromResponse(body)  (code, status, meta, requestId)
                 401 (not /login) → window event "nexus:auth-expired" → AuthContext logs out → /login?next=...
                 ALWAYS Promise.reject(apiError)   ← the original code swallowed errors here
STOMP /user/queue/errors ──► ApiError.fromResponse ──► handleError
STOMP ERROR frame        ──► AUTH_* → logout, else handleError

handleError(error, { setFieldErrors }) (lib/errorHandler.ts)
   errorMeta → Formik field errors (shown under the inputs)
   friendly text per code (or server message) → toast
   5xx → toast includes "(ref: <requestId>)"

Render crash → app/error.tsx ("Something glitched", Try again)  · root layout crash → app/global-error.tsx
WebGL crash  → SceneBoundary in component/three/Scene3D.tsx → static gradient fallback
```

## 7. Error flows (end to end)

| Scenario | Flow | User sees |
|---|---|---|
| **Invalid request** (empty message) | `@Valid` → `MethodArgumentNotValidException` → `handleValidation` → 400 `VAL_400` `{content: "..."}` | red text under the field / toast |
| **Database unavailable** | Mongo driver timeout → Spring `DataAccessResourceFailureException` → `handleDatabaseUnavailable` → 503 `DB_503` (log: `ERROR [req:ab12] Database unavailable: ...`) | "Our database is temporarily unreachable. Please retry (ref: ab12…)"; pending message bubble → retry |
| **Unauthorized** (no / bad token) | `JwtAuthenticationFilter` marks reason → Security entry point → 401 `AUTH_401` | redirected to login |
| **Expired / logged-out token** | 401 `AUTH_403` → `nexus:auth-expired` | "Your session expired" + login |
| **Forbidden** (not a member) | `AccessGuard` → `ForbiddenException(ROOM_403)` → 403 | "You are not part of this conversation" page |
| **Conversation not found** | `AccessGuard.requireRoom` → 404 `ROOM_404` | "Can't open this chat" empty state |
| **User not found** | `UserService.requireUser` → 404 `USER_404` | toast |
| **Duplicate operation** (register twice) | `DuplicateResourceException(USER_409, meta)` → 409 | error under the username field |
| **Duplicate message** (retry) | not an error: same message returned (200/201) | nothing — exactly one bubble |
| **Rate limited** | `RateLimitException` → 429 `RATE_429` | "Slow down!" toast (de-duplicated) |
| **WebSocket disconnect** | socket closes → client marks "sync", reconnects every 3 s, sends via REST meanwhile | small status change, chat keeps working |
| **Malformed WebSocket message** | conversion/validation error in `@MessageMapping` → `@MessageExceptionHandler` → `/user/queue/errors` | toast; connection stays open |
| **Unexpected bug** | `handleUnexpected` → 500 `SYS_500`, stack trace logged with request id | generic message with reference id |
