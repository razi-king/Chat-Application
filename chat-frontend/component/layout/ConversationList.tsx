"use client";
import React, { useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { MessageSquarePlus, Search, Users } from "lucide-react";
import SidebarFrame from "./SidebarFrame";
import NewDirectModal from "./NewDirectModal";
import Avatar from "@/component/ui/Avatar";
import Modal from "@/component/ui/Modal";
import { Badge, EmptyState } from "@/component/ui/Primitives";
import CreateRoomChat from "@/component/chat/CreateRoomChat";
import { useAppState } from "@/context/AppStateContext";
import { useAuth } from "@/context/AuthContext";
import { cn, formatListTime, roomTitle } from "@/lib/utils";

// WhatsApp Style Chat List
export default function ConversationList({ className = "" }: { className?: string }) {
  const { roomId } = useParams<{ roomId?: string }>();
  const router = useRouter();
  const { conversations, isOnline, loaded, refreshConversations } = useAppState();
  const { user } = useAuth();
  const [filter, setFilter] = useState("");
  const [modal, setModal] = useState<"dm" | "group" | null>(null);

  const list = useMemo(() => {
    const f = filter.trim().toLowerCase();
    return f ? conversations.filter((r) => roomTitle(r).toLowerCase().includes(f)) : conversations;
  }, [conversations, filter]);

  return (
    <SidebarFrame
      className={className}
      header={
        <div className="flex items-center justify-between w-full">
          <h2 className="font-display text-lg font-bold text-white">Chats</h2>
          <div className="flex gap-1">
            <button onClick={() => setModal("dm")} title="New chat" className="p-2 rounded-lg text-slate-400 hover:text-cyan-300 hover:bg-white/5 cursor-pointer">
              <MessageSquarePlus size={18} />
            </button>
            <button onClick={() => setModal("group")} title="New group" className="p-2 rounded-lg text-slate-400 hover:text-cyan-300 hover:bg-white/5 cursor-pointer">
              <Users size={18} />
            </button>
          </div>
        </div>
      }
    >
      <div className="p-3">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Search chats"
            className="w-full pl-9 pr-3 py-2 rounded-xl bg-white/[0.04] border border-line focus:border-cyan-400/50 outline-none text-sm placeholder-slate-500"
          />
        </div>
      </div>
      <div className="px-2 pb-3 space-y-0.5">
        {loaded && list.length === 0 && (
          <EmptyState title="No chats yet" text="Start a conversation with a friend or create a group." />
        )}
        {!loaded && Array.from({ length: 5 }).map((_, i) => <div key={i} className="h-16 rounded-xl skeleton m-1" />)}
        {list.map((room) => {
          const active = room.id === roomId;
          const isDm = room.type === "DIRECT";
          const last = room.lastMessage;
          const prefix = last?.senderId ? (last.senderId === user?.id ? "You: " : !isDm && last.senderName ? `${last.senderName.split(" ")[0]}: ` : "") : "";
          return (
            <Link
              key={room.id}
              href={`/app/chats/${room.id}`}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all",
                active ? "bg-gradient-to-r from-cyan-400/15 to-violet-400/10 shadow-[inset_0_0_0_1px_rgba(34,211,238,0.25)]" : "hover:bg-white/[0.04]"
              )}
            >
              {isDm ? (
                <Avatar name={room.otherUser?.displayName} color={room.otherUser?.avatarColor} size="lg" showStatus online={isOnline(room.otherUser)} />
              ) : (
                <Avatar name={room.name} color={room.iconColor} size="lg" square />
              )}
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className={cn("truncate text-sm", room.unreadCount > 0 ? "font-bold text-white" : "font-semibold text-slate-200")}>
                    {roomTitle(room)}
                  </span>
                  <span className={cn("text-[11px] shrink-0", room.unreadCount > 0 ? "text-cyan-300" : "text-slate-500")}>
                    {formatListTime(last?.sentAt ?? room.lastActivityAt)}
                  </span>
                </div>
                <div className="flex items-center justify-between gap-2 mt-0.5">
                  <span className={cn("truncate text-xs", room.unreadCount > 0 ? "text-slate-300" : "text-slate-500")}>
                    {last ? prefix + last.preview : isDm ? "Say hi 👋" : "Group created"}
                  </span>
                  <Badge count={room.unreadCount} />
                </div>
              </div>
            </Link>
          );
        })}
      </div>
      <NewDirectModal open={modal === "dm"} onClose={() => setModal(null)} />
      <Modal open={modal === "group"} onClose={() => setModal(null)} title="New group" subtitle="Pick friends and name your group">
        <CreateRoomChat defaultMode="group" onDone={(path) => { setModal(null); refreshConversations(); router.push(path); }} />
      </Modal>
    </SidebarFrame>
  );
}
