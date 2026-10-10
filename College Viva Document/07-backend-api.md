# 7. Backend API

Base URL: `/api/v1`. Live documentation: `/swagger-ui.html`. All endpoints except register, login, invite preview and health need `Authorization: Bearer <token>`.

## Main endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/register` | Create an account |
| POST | `/auth/login` | Log in, get a JWT |
| POST | `/auth/logout` | Revoke the token |
| GET | `/users/me` / PUT `/users/me` | My profile / update it |
| GET | `/users/search?q=` | Find people |
| GET | `/rooms` | My chats (DMs + groups) with unread counts |
| POST | `/rooms/direct` | Open or create a DM |
| POST | `/rooms/group` | Create a group |
| GET | `/rooms/{id}/messages?before=&size=30` | Chat history (cursor pagination) |
| POST | `/rooms/{id}/messages` | Send a message (REST fallback) |
| POST | `/rooms/{id}/read` | Mark as read (blue ticks) |
| PUT / DELETE | `/messages/{id}` | Edit / delete a message |
| POST | `/messages/{id}/reactions` | Toggle a reaction |
| POST | `/messages/{id}/pin` | Toggle pin |
| GET / POST | `/servers` | My servers / create a server |
| GET | `/servers/{id}` | Server details: channels and members |
| POST | `/servers/{id}/channels` | Create a channel (Admin+) |
| DELETE | `/servers/{id}/members/{userId}` | Kick (Admin+, lower roles only) |
| PUT | `/servers/{id}/members/{userId}/role` | Change role (Owner) |
| POST | `/servers/{id}/invites` | Create an invite link |
| POST | `/invites/{code}/accept` | Join with an invite |
| GET / POST | `/friends`, `/friends/requests` | Friends and requests |
| GET | `/notifications` | Notification inbox |
| GET | `/health` | Health check (public) |

## Example: send a message
Request:
```json
POST /api/v1/rooms/6abf.../messages
{ "content": "See you at 5 @kabir", "replyToId": null, "clientMessageId": "c-3f1d9a0e-8c2b" }
```
Response (201):
```json
{ "success": true, "message": "Message Sent",
  "data": { "id": "6abf85f6...", "content": "See you at 5 @kabir",
            "sender": { "displayName": "Razi Khan", "online": true },
            "clientMessageId": "c-3f1d9a0e-8c2b", "reactions": [], "createdAt": "2026-10-02T10:22:46Z" } }
```
Sending the same `clientMessageId` again returns the **same** message id (no duplicate).

## Example: validation error
```json
POST /api/v1/auth/register  { "username": "a", "email": "bad", "password": "1", "displayName": "" }
-> 400
{ "success": false, "errorCode": "VAL_400", "message": "Please fix the highlighted fields",
  "errorMeta": { "username": "Username must be 3-20 characters", "email": "Enter a valid email",
                 "password": "Password must be at least 6 characters", "displayName": "Display name is required" } }
```

## API design rules followed
- Nouns for resources, sub-paths for actions (`/messages/{id}/pin`), version in the path (`/v1`).
- The acting user always comes from the JWT, never from the request body.
- The same response envelope for success and errors.
- Safe to repeat: join, open DM, accept invite, send with the same clientMessageId.
