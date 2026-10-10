# 8. Frontend

Built with **Next.js 16 (App Router), React 19 and TypeScript**, styled with **Tailwind CSS 4**.

## Pages

| Route | Page |
|---|---|
| `/` | 3D landing page |
| `/login`, `/register` | Login / sign up (3D side panel) |
| `/app` | Dashboard: unread count, online friends, servers, notifications |
| `/app/chats`, `/app/chats/{id}` | WhatsApp-style chat list and chat |
| `/app/servers`, `/app/servers/{id}/{channel}` | Discord-style servers and channels |
| `/app/friends`, `/app/discover`, `/app/notifications`, `/app/profile` | Friends, public servers, inbox, profile |
| `/invite/{code}` | Invite preview and join |

## How the frontend is organised

| Folder | Contains |
|---|---|
| `app/` | Pages (routes) |
| `component/` | UI: chat window, sidebars, forms, 3D scenes, style enums |
| `context/` | Shared state: Auth, Socket, AppState, Server |
| `services/` | One class per backend area (`RoomService`, `ServerService` ...) |
| `lib/` | `ApiError`, `errorHandler`, helpers |
| `types/` | TypeScript copies of the backend DTOs |

**Contexts (shared state):**
- `AuthContext`: token + current user; logs out automatically when the token expires.
- `SocketContext`: one STOMP WebSocket connection; re-subscribes after reconnect.
- `AppStateContext`: chat list, servers, unread badges, who is online.

**Reusable form:** `ReusableForm` + `InputField` (Formik + Yup) build every form (login, register, create server ...). The same validation rules as the backend give instant feedback; the backend still checks again.

## Chat window (`ChatPage`)
- Optimistic sending (bubble shows instantly) with retry on failure.
- Infinite scroll upward to load older messages.
- Bubbles (WhatsApp style) for DMs/groups, flat rows (Discord style) for channels.
- Reply, react, edit, delete, pin, copy; @mention suggestions; emoji picker.
- Read ticks: one tick = sent, blue double tick = read.

## 3D design
Three.js (via React Three Fiber) draws a glowing crystal with orbit rings, floating chat bubbles and stars on the landing and login pages. It is loaded only in the browser, uses fewer particles on phones, and if WebGL fails an error boundary shows a gradient instead. The chat screens use plain CSS so they stay fast.

## Mobile layout
- Under 768px the Discord-style side rail becomes a **bottom tab bar** (Home, Chats, Servers, Alerts, Me).
- The tab bar hides inside an open chat, like WhatsApp.
- Tap a message to see its actions (phones have no hover).
- Inputs use 16px text so iPhones do not zoom in.
