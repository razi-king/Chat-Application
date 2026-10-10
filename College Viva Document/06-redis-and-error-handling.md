# 6. Redis and error handling

## Why Redis?
MongoDB is the **permanent memory** (users, messages). Redis is the **short-term memory** for small numbers that change on almost every action. Redis keeps data in RAM, has **atomic** commands (`INCR`, `HINCRBY`) so two updates at the same time are never lost, and keys can **expire automatically** (TTL).

| Redis key | Type | Used for | When it changes |
|---|---|---|---|
| `presence:connections` | hash: userId -> number of open connections | Online / offline | +1 on connect, -1 on disconnect |
| `unread:<userId>` | hash: roomId -> unread count | Unread badges | +1 for others when a message is sent; removed when the chat is opened |
| `rate:<action>:<user>:<window>` | number with expiry | Rate limiting | Counts messages (5 s window) and logins (60 s window) |
| `auth:blacklist:<tokenId>` | string with expiry | Logout | Saved on logout, deleted automatically when the token would expire |

**What if Redis goes down?** Every Redis call goes through the `RedisSafe` class:
- presence moves to memory;
- unread counts are calculated from MongoDB (messages after `lastReadAt`);
- rate limiting lets requests through;
- Redis is skipped for 30 seconds so the app does not slow down retrying.

The chat keeps working. (Tested: Redis stopped, messages still sent.)

## Error handling
All errors follow **one JSON format**:
```json
{ "success": false, "message": "Room not found", "errorCode": "ROOM_404",
  "requestId": "9f1c2a7b3d4e5f60", "timestamp": "02-10-2026 15:52:46" }
```

**How it works:**
1. Services **throw** custom exceptions, for example `new ForbiddenException(ErrorCode.NOT_ROOM_MEMBER)`.
2. Every custom exception extends `ChatAppException`, which carries an `ErrorCode`.
3. The `ErrorCode` enum knows its HTTP status (`ROOM_404` -> 404).
4. One global handler (`RestResponseExceptionHandler`, a `@ControllerAdvice`) converts every exception into the JSON above.
5. WebSocket errors use the same format, sent to `/user/queue/errors`.

**Exception classes** (only 6, on purpose): `ResourceNotFoundException`, `DuplicateResourceException`, `UnauthorizedException`, `ForbiddenException`, `BadRequestException`, `RateLimitException`. The status lives in the enum, so a new error is one enum line.

| Status | Example codes | Meaning |
|---|---|---|
| 400 | VAL_400, REQ_400 | Invalid input (fields listed in `errorMeta`) |
| 401 | AUTH_401, AUTH_402, AUTH_403 | Not logged in / wrong password / session expired |
| 403 | AUTH_404, ROOM_403, SRV_403 | Logged in but not allowed |
| 404 | USER_404, ROOM_404, SRV_404 | Not found |
| 409 | USER_409, USER_410, FRD_409 | Duplicate (username taken ...) |
| 429 | RATE_429 | Too many requests |
| 503 | DB_503 | Database temporarily down: retry |
| 500 | SYS_500 | Unexpected bug (details only in logs) |

**requestId:** `RequestIdFilter` gives every request an id. It is in the `X-Request-Id` header, in every log line and in error responses. A user can quote it and the exact log lines can be found.

**Frontend side:** every failure becomes one `ApiError` type, and one function (`handleError`) shows a friendly message, puts field errors under the right input, and logs the user out on 401.
