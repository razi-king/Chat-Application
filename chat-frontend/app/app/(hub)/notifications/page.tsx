"use client";
import React, { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AtSign, Bell, CheckCheck, UserCheck, UserPlus, Users, Server } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { EmptyState, Spinner } from "@/component/ui/Primitives";
import { FormButton } from "@/component/enums/ButtonStyles";
import { NotificationService } from "@/services/SocialService";
import { useAppState } from "@/context/AppStateContext";
import { useSocket } from "@/context/SocketContext";
import { handleError } from "@/lib/errorHandler";
import { cn, timeAgo } from "@/lib/utils";
import type { Notification, NotificationType } from "@/types";

const icons: Record<NotificationType, React.ReactNode> = {
  FRIEND_REQUEST: <UserPlus size={14} />,
  FRIEND_ACCEPTED: <UserCheck size={14} />,
  MENTION: <AtSign size={14} />,
  ADDED_TO_GROUP: <Users size={14} />,
  SERVER_JOINED: <Server size={14} />,
};

export default function NotificationsPage() {
  const router = useRouter();
  const { setNotificationCount } = useAppState();
  const { onEvent } = useSocket();
  const [items, setItems] = useState<Notification[] | null>(null);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  const load = useCallback(async (p: number) => {
    try {
      const res = await NotificationService.list(p);
      setItems((prev) => (p === 0 ? res.items : [...(prev ?? []), ...res.items]));
      setHasNext(res.hasNext);
      setPage(p);
    } catch (e) {
      handleError(e);
      setItems((prev) => prev ?? []);
    }
  }, []);

  useEffect(() => {
    // Data Fetch On Mount: State Is Only Set After The Request Resolves
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(0);
  }, [load]);
  useEffect(() => onEvent((e) => { if (e.type === "NOTIFICATION") load(0); }), [onEvent, load]);

  const open = async (n: Notification) => {
    if (!n.read) {
      NotificationService.markRead(n.id).catch(() => {});
      setItems((list) => list?.map((x) => (x.id === n.id ? { ...x, read: true } : x)) ?? null);
      setNotificationCount((c) => Math.max(0, c - 1));
    }
    if (n.link) router.push(n.link);
  };

  const readAll = async () => {
    try {
      await NotificationService.markAllRead();
      setItems((list) => list?.map((x) => ({ ...x, read: true })) ?? null);
      setNotificationCount(0);
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <div className="p-6 lg:p-10 max-w-3xl">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-3xl font-bold text-white flex items-center gap-3"><Bell className="text-pink-300" /> Notifications</h1>
        <button onClick={readAll} className={`${FormButton.GHOST} text-sm flex items-center gap-1.5 cursor-pointer`}><CheckCheck size={16} /> Mark all read</button>
      </div>
      <div className="mt-8 space-y-2">
        {items === null && <div className="flex justify-center py-10"><Spinner size={28} /></div>}
        {items?.length === 0 && <EmptyState icon={<Bell size={26} />} title="All caught up" text="Mentions, friend requests and group invites show up here." />}
        {items?.map((n) => (
          <button key={n.id} onClick={() => open(n)}
            className={cn("w-full text-left rounded-2xl px-4 py-3 flex items-start gap-4 transition-colors cursor-pointer",
              n.read ? "glass-soft opacity-70 hover:opacity-100" : "glass neon-border")}>
            <div className="relative">
              <Avatar name={n.actor?.displayName} color={n.actor?.avatarColor} />
              <span className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-[#0a0c19] border border-line flex items-center justify-center text-cyan-300">{icons[n.type]}</span>
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-sm font-semibold text-white">{n.title}</div>
              <div className="text-sm text-slate-400 truncate">{n.body}</div>
              <div className="text-[11px] text-slate-500 mt-1">{timeAgo(n.createdAt)}</div>
            </div>
            {!n.read && <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 shadow-[0_0_8px_#22d3ee] mt-2" />}
          </button>
        ))}
        {hasNext && (
          <button onClick={() => load(page + 1)} className={`${FormButton.SECONDARY} w-full mt-4 cursor-pointer`}>Load more</button>
        )}
      </div>
    </div>
  );
}
