// Mirrors The Backend DTOs (com.example.crm.dto) So Both Sides Speak The Same Shapes

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
  message: string;
  errorCode?: string;
  requestId?: string;
  errorMeta?: Record<string, string> | unknown;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  hasNext: boolean;
}

export type MemberRole = "OWNER" | "ADMIN" | "MEMBER";
export type RoomType = "DIRECT" | "GROUP" | "CHANNEL";
export type MessageType = "TEXT" | "SYSTEM";
export type FriendshipStatus = "PENDING" | "ACCEPTED";
export type NotificationType = "FRIEND_REQUEST" | "FRIEND_ACCEPTED" | "MENTION" | "ADDED_TO_GROUP" | "SERVER_JOINED";

export interface User {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  avatarColor: string;
  about?: string;
  customStatus?: string;
  online: boolean;
  lastSeenAt?: string;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface LastMessage {
  messageId: string;
  senderId?: string;
  senderName?: string;
  preview: string;
  sentAt: string;
}

export interface RoomMember {
  user: User;
  role?: MemberRole;
  lastReadAt?: string;
  joinedAt?: string;
}

export interface Room {
  id: string;
  name?: string;
  description?: string;
  type: RoomType;
  serverId?: string;
  iconColor?: string;
  createdBy?: string;
  position: number;
  lastMessage?: LastMessage;
  unreadCount: number;
  myRole?: MemberRole;
  muted: boolean;
  otherUser?: User;
  members?: RoomMember[];
  createdAt?: string;
  lastActivityAt?: string;
}

export interface Server {
  id: string;
  name: string;
  description?: string;
  iconColor: string;
  ownerId: string;
  discoverable: boolean;
  memberCount: number;
  myRole?: MemberRole;
  unreadCount: number;
  createdAt?: string;
}

export interface ServerMember {
  user: User;
  role: MemberRole;
  nickname?: string;
  joinedAt?: string;
}

export interface ServerDetail {
  server: Server;
  channels: Room[];
  members: ServerMember[];
}

export interface Reaction {
  emoji: string;
  count: number;
  userIds: string[];
}

export interface ReplyPreview {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  deleted: boolean;
}

export interface Message {
  id: string;
  roomId: string;
  clientMessageId?: string;
  sender?: User;
  content: string;
  type: MessageType;
  replyTo?: ReplyPreview;
  reactions: Reaction[];
  mentions: string[];
  edited: boolean;
  deleted: boolean;
  pinned: boolean;
  createdAt: string;
  editedAt?: string;
  // Client Only: Optimistic Message Not Yet Confirmed By The Server
  pending?: boolean;
  failed?: boolean;
}

export interface Invite {
  code: string;
  serverId: string;
  serverName: string;
  serverDescription?: string;
  serverIconColor: string;
  memberCount: number;
  createdBy?: User;
  maxUses: number;
  uses: number;
  expiresAt?: string;
  createdAt: string;
}

export interface Friendship {
  id: string;
  user: User;
  status: FriendshipStatus;
  incoming: boolean;
  createdAt: string;
}

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  link?: string;
  actor?: User;
  read: boolean;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  actor?: User;
  action: string;
  targetId?: string;
  details?: string;
  createdAt: string;
}

export interface Dashboard {
  friends: number;
  pendingRequests: number;
  servers: number;
  conversations: number;
  unreadMessages: number;
  unreadNotifications: number;
  onlineFriends: number;
}

// ---------- WebSocket Events (com.example.crm.dto.event) ----------
export type ChatEventType =
  | "MESSAGE_CREATED"
  | "MESSAGE_UPDATED"
  | "MESSAGE_DELETED"
  | "TYPING"
  | "READ"
  | "PRESENCE"
  | "NOTIFICATION"
  | "ROOM_UPDATED"
  | "UNREAD"
  | "ERROR";

export interface ChatEvent<T = unknown> {
  type: ChatEventType;
  roomId?: string;
  payload: T;
  timestamp: string;
}

export interface TypingEvent {
  userId: string;
  displayName: string;
  typing: boolean;
}

export interface ReadEvent {
  userId: string;
  lastReadAt: string;
}

export interface PresenceEvent {
  userId: string;
  online: boolean;
  lastSeenAt?: string;
}

export interface UnreadEvent {
  roomId: string;
  serverId?: string;
  unreadCount: number;
}

export interface RoomUpdatedPayload {
  action: string;
  serverId?: string;
}
