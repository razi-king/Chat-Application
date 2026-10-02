"use client";
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { useSocket } from "./SocketContext";
import { RoomService } from "@/services/RoomService";
import { ServerService } from "@/services/ServerService";
import { NotificationService } from "@/services/SocialService";
import { handleError } from "@/lib/errorHandler";
import type { Notification, PresenceEvent, Room, RoomUpdatedPayload, Server, UnreadEvent, User } from "@/types";

interface AppState {
  conversations: Room[];
  servers: Server[];
  loaded: boolean;
  notificationCount: number;
  setNotificationCount: React.Dispatch<React.SetStateAction<number>>;
  refreshConversations: () => Promise<void>;
  refreshServers: () => Promise<void>;
  isOnline: (user?: User) => boolean;
  /** The chat page tells us which room is open so its badge stays at 0. */
  setActiveRoom: (roomId: string | null) => void;
  clearUnread: (roomId: string) => void;
  /** Bumped when a server's channels / members change; server pages re-fetch on change. */
  serverVersion: Record<string, number>;
}

const AppStateContext = createContext<AppState | null>(null);

export function AppStateProvider({ children }: { children: React.ReactNode }) {
  const { onEvent } = useSocket();
  const router = useRouter();
  const pathname = usePathname();
  const [conversations, setConversations] = useState<Room[]>([]);
  const [servers, setServers] = useState<Server[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [notificationCount, setNotificationCount] = useState(0);
  const [presence, setPresence] = useState<Record<string, boolean>>({});
  const [serverVersion, setServerVersion] = useState<Record<string, number>>({});
  const activeRoom = useRef<string | null>(null);
  const pathnameRef = useRef(pathname);
  useEffect(() => {
    pathnameRef.current = pathname;
  }, [pathname]);

  const refreshConversations = useCallback(async () => {
    try {
      setConversations(await RoomService.myConversations());
    } catch (e) {
      handleError(e, { silent: true });
    }
  }, []);

  const refreshServers = useCallback(async () => {
    try {
      setServers(await ServerService.mine());
    } catch (e) {
      handleError(e, { silent: true });
    }
  }, []);

  useEffect(() => {
    // Initial Data Fetch: Every setState Happens After A Request Resolves
    Promise.all([
      // eslint-disable-next-line react-hooks/set-state-in-effect
      refreshConversations(),
      refreshServers(),
      NotificationService.unreadCount().then((r) => setNotificationCount(r.count)).catch(() => {}),
    ]).finally(() => setLoaded(true));
  }, [refreshConversations, refreshServers]);

  // Several Events Can Arrive Together -> Refetch Lists At Most Every 400ms
  const timers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  const debounced = useCallback((key: string, fn: () => void) => {
    clearTimeout(timers.current[key]);
    timers.current[key] = setTimeout(fn, 400);
  }, []);

  const bumpServer = useCallback((serverId: string) => {
    setServerVersion((v) => ({ ...v, [serverId]: (v[serverId] ?? 0) + 1 }));
  }, []);

  useEffect(() => {
    return onEvent((event) => {
      switch (event.type) {
        case "UNREAD": {
          const p = event.payload as UnreadEvent;
          if (p.roomId === activeRoom.current && p.unreadCount > 0) {
            // We Are Looking At This Room -> Read Immediately, Then Refresh The Previews
            RoomService.markRead(p.roomId)
              .catch(() => {})
              .finally(() => debounced(p.serverId ? "servers" : "conversations", p.serverId ? refreshServers : refreshConversations));
            return;
          }
          if (p.serverId) {
            debounced("servers", refreshServers);
            bumpServer(p.serverId);
          } else {
            debounced("conversations", refreshConversations);
          }
          break;
        }
        case "ROOM_UPDATED": {
          const p = event.payload as RoomUpdatedPayload;
          if (p.serverId) {
            debounced("servers", refreshServers);
            bumpServer(p.serverId);
            if ((p.action === "kicked" || p.action === "server-deleted") && pathnameRef.current.includes(p.serverId)) {
              toast.error(p.action === "kicked" ? "You were removed from this server" : "This server was deleted");
              router.replace("/app");
            }
          } else {
            debounced("conversations", refreshConversations);
            if (p.action === "removed" && event.roomId && pathnameRef.current.includes(event.roomId)) {
              toast.error("You are no longer in this group");
              router.replace("/app/chats");
            }
          }
          break;
        }
        case "NOTIFICATION": {
          const n = event.payload as Notification;
          setNotificationCount((c) => c + 1);
          toast(
            (t) => (
              <button
                className="text-left"
                onClick={() => {
                  toast.dismiss(t.id);
                  if (n.link) router.push(n.link);
                }}
              >
                <div className="font-semibold text-white">{n.title}</div>
                <div className="text-sm text-slate-400">{n.body}</div>
              </button>
            ),
            { icon: "🔔", duration: 5000 }
          );
          break;
        }
        case "PRESENCE": {
          const p = event.payload as PresenceEvent;
          setPresence((prev) => ({ ...prev, [p.userId]: p.online }));
          break;
        }
      }
    });
  }, [onEvent, debounced, refreshConversations, refreshServers, bumpServer, router]);

  const isOnline = useCallback((user?: User) => (user ? presence[user.id] ?? user.online : false), [presence]);

  const setActiveRoom = useCallback((roomId: string | null) => {
    activeRoom.current = roomId;
  }, []);

  const clearUnread = useCallback((roomId: string) => {
    setConversations((list) => list.map((r) => (r.id === roomId ? { ...r, unreadCount: 0 } : r)));
  }, []);

  const value = useMemo(
    () => ({
      conversations,
      servers,
      loaded,
      notificationCount,
      setNotificationCount,
      refreshConversations,
      refreshServers,
      isOnline,
      setActiveRoom,
      clearUnread,
      serverVersion,
    }),
    [conversations, servers, loaded, notificationCount, refreshConversations, refreshServers, isOnline, setActiveRoom, clearUnread, serverVersion]
  );
  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>;
}

export function useAppState(): AppState {
  const ctx = useContext(AppStateContext);
  if (!ctx) throw new Error("useAppState must be used inside <AppStateProvider>");
  return ctx;
}
