# 1. Project overview

## What is Nexus?
Nexus is a **real-time chat application** that combines two kinds of apps students already use:

- **WhatsApp side**: private 1-to-1 chats and groups, read receipts, typing indicator, last seen.
- **Discord side**: servers (communities) with text channels, roles, invite links and an audit log.

Everything updates **live**: when someone sends a message, it appears on the other person's screen instantly, without refreshing.

## The problem
1. Students switch between two apps (WhatsApp for friends, Discord for class communities): two accounts and scattered conversations.
2. Simple chat projects ignore real-world problems:
   - a message is **lost** if the receiver is offline;
   - a message is **duplicated** when the network retries;
   - anyone can read or post in any room (no permissions).

## Objectives
1. One app for DMs, groups, servers and channels.
2. Real-time delivery with read ticks, typing and online status.
3. Secure login (JWT) and role-based permissions (RBAC).
4. No lost messages and no duplicate messages.
5. A clean, documented, tested and deployed system.

## Features

| Area | Features |
|---|---|
| Accounts | Register, login with username or email, logout (token really revoked), edit profile and avatar colour |
| People | Search users, friend requests, online / last seen |
| Chats (WhatsApp) | 1-to-1 chats, groups, add/remove members, read ticks (blue when read), typing, unread badges |
| Servers (Discord) | Servers, channels, Owner/Admin/Member roles, kick, promote, invite links, Discover, audit log |
| Messages | Live delivery, history with infinite scroll, reply, react, edit, delete, pin, search, @mentions |
| UI | 3D landing page (Three.js), dark futuristic theme, works on desktop and mobile |

## The whole system in one picture

```
 Phone / Laptop browser
        |  HTTPS (REST)  +  WSS (WebSocket)
        v
 Frontend: Next.js on Vercel
        |
        v
 Backend: Spring Boot in Docker on Render
   JWT filter -> Controllers -> Services (+ AccessGuard) -> Repositories
        |                                   |
        v                                   v
 MongoDB Atlas (permanent data)        Upstash Redis (fast counters)
```

## Technology stack

| Layer | Technology | Why |
|---|---|---|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 | Typed components, fast routing, easy deploy |
| 3D / animation | Three.js (React Three Fiber), Framer Motion | Futuristic landing page |
| Backend | Java 17, Spring Boot 3.5 | Layered design, dependency injection, security |
| Database | MongoDB | Chat messages are naturally documents |
| Cache | Redis | Fast atomic counters with automatic expiry |
| Real-time | WebSocket + STOMP | Server can push messages instantly |
| Security | Spring Security, JWT, BCrypt | Stateless login for REST and WebSocket |
| Tests | JUnit 5, Mockito, MockMvc | 33 automated tests |
| Hosting | Vercel, Render (Docker), MongoDB Atlas, Upstash | Free cloud deployment |

## Where is the code?

| Folder | Contains |
|---|---|
| `ChatAppBackend/` | Spring Boot API (Java) |
| `chat-frontend/` | Next.js website |
| `docs/` | Detailed engineering documentation |
| `docker-compose.yml` | Starts MongoDB + Redis locally |
| `render.yaml` | Render deployment settings |
