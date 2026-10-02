import type { Room, User } from "@/types";

// Unique Id For Optimistic Messages (Also The Idempotency Key). crypto.randomUUID Needs HTTPS Or
// localhost, So A Phone Opening http://192.168.x.x Falls Back To getRandomValues / Math.random.
export function newClientId(): string {
  const c = typeof crypto !== "undefined" ? crypto : undefined;
  if (c?.randomUUID) return `c-${c.randomUUID()}`;
  if (c?.getRandomValues) {
    const bytes = c.getRandomValues(new Uint8Array(16));
    return `c-${Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("")}`;
  }
  return `c-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}

export function initials(name?: string): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase() || "?";
}

export function formatTime(iso?: string): string {
  if (!iso) return "";
  return new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

export function formatDay(iso: string): string {
  const d = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return "Today";
  if (d.toDateString() === yesterday.toDateString()) return "Yesterday";
  return d.toLocaleDateString([], { weekday: "long", day: "numeric", month: "short", year: d.getFullYear() === today.getFullYear() ? undefined : "numeric" });
}

// Chat List Style: "14:05" Today, "Yesterday", Or "12/09"
export function formatListTime(iso?: string): string {
  if (!iso) return "";
  const d = new Date(iso);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) return formatTime(iso);
  const diffDays = (now.getTime() - d.getTime()) / 86_400_000;
  if (diffDays < 2) return "Yesterday";
  if (diffDays < 7) return d.toLocaleDateString([], { weekday: "short" });
  return d.toLocaleDateString([], { day: "2-digit", month: "2-digit" });
}

export function timeAgo(iso?: string): string {
  if (!iso) return "";
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

export function roomTitle(room: Room): string {
  if (room.type === "DIRECT") return room.otherUser?.displayName ?? "Direct chat";
  if (room.type === "CHANNEL") return room.name ?? "channel";
  return room.name ?? "Group";
}

export function lastSeenText(user?: User, online?: boolean): string {
  if (!user) return "";
  if (online) return "online";
  return user.lastSeenAt ? `last seen ${timeAgo(user.lastSeenAt)}` : "offline";
}

export const QUICK_REACTIONS = ["👍", "❤️", "😂", "😮", "😢", "🔥"];

export const EMOJIS = [
  "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎", "🤩", "🥳", "😇", "🙂",
  "😉", "😜", "🤔", "🤨", "😐", "🙄", "😏", "😴", "🤯", "😱", "😭", "😡",
  "🥺", "😅", "🤗", "🤝", "👍", "👎", "👏", "🙌", "🙏", "💪", "👀", "🧠",
  "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "💯", "🔥", "✨", "⚡", "🚀",
  "🎉", "🎮", "🎧", "📚", "💻", "☕", "🍕", "🌙", "⭐", "✅", "❌", "⚠️",
];

export const PALETTE = ["#22d3ee", "#a78bfa", "#f472b6", "#34d399", "#fbbf24", "#60a5fa", "#fb7185", "#2dd4bf", "#c084fc", "#f97316"];
