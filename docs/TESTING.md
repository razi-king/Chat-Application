# Testing

```bash
cd ChatAppBackend
./mvnw test            # Windows: .\mvnw.cmd test
```
Current result: **28 tests, 0 failures** (integration tests run when MongoDB is reachable on `localhost:27017`, otherwise they are skipped).

## 1. Strategy

| Level | Tool | Speed | What it proves |
|---|---|---|---|
| Unit | JUnit 5 + Mockito | ms | Business rules in services, in isolation (repositories/Redis/WebSocket mocked) |
| Web / error flow | MockMvc standalone + the real `RestResponseExceptionHandler` | ms | The HTTP contract of errors: status, code, envelope, no leaks |
| Security unit | JUnit (real `JwtService`) | ms | Token signing, forgery, expiry |
| Concurrency | JUnit + thread pool + `CountDownLatch` | < 1 s | Race-free presence counting |
| Integration | `@SpringBootTest` + real MongoDB (`chatapp_test` DB, dropped afterwards) | seconds | Unique indexes really enforce idempotency under concurrent load |

**Why mostly unit tests?** The rules live in services; mocking the edges makes failures point at one method. Integration tests are kept for things mocks
**cannot** prove — e.g. that MongoDB's unique index actually rejects the concurrent duplicate.

## 2. What each test protects against

### `AuthServiceImplTest`
| Test | Protects against |
|---|---|
| `register_valid_hashesPasswordAndNormalisesIdentity` | Storing plaintext passwords; "Razi" and "razi" becoming two accounts |
| `register_duplicateUsername_conflictWithFieldMeta` | Duplicate registration; losing the field info the form needs |
| `register_duplicateEmail_conflict` | Two accounts per email |
| `login_validByEmail_returnsToken` | Login by email breaking due to case differences |
| `login_wrongPasswordAndUnknownUser_sameGenericError` | **User enumeration** (different errors would reveal which usernames exist); issuing tokens on failure |
| `login_rateLimited_rejectedBeforePasswordCheck` | Brute force; wasting BCrypt CPU on blocked attempts |

### `ChatServiceImplTest`
| Test | Protects against |
|---|---|
| `sendMessage_valid_persistsBroadcastsAndCountsUnreadForOthersOnly` | Sender getting an unread badge for their own message; skipping delivery |
| `sendMessage_notMember_forbiddenAndNothingSaved` | **Forbidden access**: posting into someone else's conversation |
| `sendMessage_unknownRoom_notFound` | **Conversation not found** returning 500 or saving orphan messages |
| `sendMessage_duplicateClientMessageId_returnsOriginalWithoutSaving` | **Duplicate message** on network retry; double unread; retries burning rate limit |
| `sendMessage_concurrentDuplicate_uniqueIndexLoserReturnsWinner` | Concurrent duplicate turning into an error for the client |
| `sendMessage_rateLimited_nothingSaved` | Spam bypassing the limiter |
| `sendMessage_replyToMessageOfAnotherRoom_badRequest` | Data leak through quoting a message from a private room |
| `sendMessage_databaseDown_exceptionPropagatesAndNothingDelivered` | **Database failure**: delivering a "ghost" message that was never stored |
| `edit_someoneElsesMessage_forbidden` | Editing other people's messages |

### `RestResponseExceptionHandlerTest`
| Test | Protects against |
|---|---|
| `customNotFound_maps404WithCode`, `customForbidden_maps403` | Wrong HTTP status for domain errors (the original bug: everything was 400) |
| `invalidBody_400WithFieldErrors` | **Invalid message** without field-level feedback |
| `malformedJson_400` | Broken JSON producing a 500 |
| `databaseUnavailable_503WithoutInternalDetails` | Leaking `mongo:27017` / driver internals; telling clients "bug" instead of "retry" |
| `unexpectedException_500WithoutLeakingMessage` | Leaking internal exception text to clients |

### `JwtServiceTest`
| Test | Protects against |
|---|---|
| `generatedToken_parsesBackToSameUser` | Wrong subject/claims; missing `jti` (logout would be impossible) |
| `tokenFromAnotherSecret_rejected` | **Forged tokens** |
| `expiredToken_tokenExpiredCode` | Accepting expired tokens; losing the "session expired" distinction |
| `garbage_rejected` | Crashes on malformed tokens |

### `PresenceServiceConcurrencyTest`
64 threads connect the same user at once, then disconnect at once (Redis forced down → in-memory fallback).
Protects against: **race conditions** in online/offline state — duplicate "online" events, flickering, or a user stuck online. While writing this test a real race was found and fixed (cleanup erasing a concurrent reconnect).

### `ChatAppBackendApplicationTests` (real MongoDB)
* `contextLoads` — the whole application wires up with real infrastructure.
* `concurrentRetriesOfSameMessage_storeExactlyOneCopy` — 10 threads send the **same** `clientMessageId` simultaneously; asserts every caller gets the same id and **exactly one** document exists.
  This test found a real problem: concurrent duplicates were consuming rate-limit quota and some retries were rejected with 429. The fix (count only after a successful, non-duplicate save) is documented in `RateLimitService`.

## 3. Not covered yet (honest list)

* Controller + full Spring Security slice tests for every endpoint (the error contract and JWT are covered; endpoint authorization rules are covered at service level).
* STOMP end-to-end tests with a real WebSocket client (`WebSocketStompClient`).
* Frontend component tests (React Testing Library) and E2E (Playwright). The chat flow was verified manually in the browser.
* Load tests (Gatling/k6) for the 10k-user scenario.

## 4. Writing a new test — checklist

1. Name it `method_condition_expectedResult`.
2. Add a comment saying **what bug it protects against**.
3. Assert on side effects that must *not* happen (`verify(repo, never()).save(...)`), not only on the return value.
4. For a race: start all threads on a `CountDownLatch`, use more threads than cores, assert the invariant (count == 1), not the timing.
