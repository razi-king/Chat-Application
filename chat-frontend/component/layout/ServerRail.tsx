"use client";
import React, { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Compass, LogIn, MessageCircle, Plus } from "lucide-react";
import { useAppState } from "@/context/AppStateContext";
import { useAuth } from "@/context/AuthContext";
import { useSocket } from "@/context/SocketContext";
import { Badge, Logo } from "@/component/ui/Primitives";
import Avatar from "@/component/ui/Avatar";
import Modal from "@/component/ui/Modal";
import CreateRoomChat from "@/component/chat/CreateRoomChat";
import JoinRoomChat from "@/component/chat/JoinRoomChat";
import { cn, initials } from "@/lib/utils";

// Discord Style Vertical Rail: Home, Chats, Servers, Create / Join / Discover
export default function ServerRail() {
  const pathname = usePathname();
  const router = useRouter();
  const { servers, conversations, refreshServers, refreshConversations } = useAppState();
  const { user } = useAuth();
  const { connected } = useSocket();
  const [modal, setModal] = useState<"create" | "join" | null>(null);
  const chatUnread = conversations.reduce((sum, r) => sum + r.unreadCount, 0);

  const done = (path: string) => {
    setModal(null);
    refreshServers();
    refreshConversations();
    router.push(path);
  };

  return (
    <nav className="hidden md:flex w-[72px] shrink-0 flex-col items-center gap-2 py-3 bg-[#05060f]/90 border-r border-line overflow-y-auto">
      <RailItem href="/app" label="Home" active={pathname === "/app" || ["/app/friends", "/app/discover", "/app/notifications", "/app/profile"].some((p) => pathname.startsWith(p))}>
        <Logo size={24} withText={false} />
      </RailItem>
      <RailItem href="/app/chats" label="Chats" active={pathname.startsWith("/app/chats")} badge={chatUnread}>
        <MessageCircle size={22} />
      </RailItem>

      <div className="w-8 h-px bg-line my-1" />

      {servers.map((s) => (
        <RailItem key={s.id} href={`/app/servers/${s.id}`} label={s.name} active={pathname.startsWith(`/app/servers/${s.id}`)} badge={s.unreadCount} color={s.iconColor}>
          <span className="font-display font-bold text-sm">{initials(s.name)}</span>
        </RailItem>
      ))}

      <RailButton label="Create server or group" onClick={() => setModal("create")}>
        <Plus size={22} />
      </RailButton>
      <RailButton label="Join with invite" onClick={() => setModal("join")}>
        <LogIn size={20} />
      </RailButton>
      <RailItem href="/app/discover" label="Discover servers" active={pathname.startsWith("/app/discover")}>
        <Compass size={22} />
      </RailItem>

      <div className="mt-auto flex flex-col items-center gap-3 pt-3">
        <div title={connected ? "Realtime connected" : "Reconnecting..."} className="flex items-center gap-1 text-[9px] font-mono uppercase tracking-wider text-slate-500">
          <span className={cn("w-2 h-2 rounded-full", connected ? "bg-emerald-400 shadow-[0_0_8px_#34d399]" : "bg-amber-400 animate-pulse")} />
          {connected ? "live" : "sync"}
        </div>
        <Link href="/app/profile" title="Your profile">
          <Avatar name={user?.displayName} color={user?.avatarColor} size="md" showStatus online={connected} />
        </Link>
      </div>

      <Modal open={modal === "create"} onClose={() => setModal(null)} title="Create" subtitle="A server for your community, or a group for your friends">
        <CreateRoomChat onDone={done} />
      </Modal>
      <Modal open={modal === "join"} onClose={() => setModal(null)} title="Join a server" subtitle="Paste an invite code or link">
        <JoinRoomChat onDone={done} />
      </Modal>
    </nav>
  );
}

function RailItem({
  href, label, active, badge = 0, color, children,
}: { href: string; label: string; active: boolean; badge?: number; color?: string; children: React.ReactNode }) {
  return (
    <div className="relative group w-full flex justify-center">
      {/* Active / Unread Indicator Pill (Like Discord) */}
      <motion.span
        className="absolute left-0 top-1/2 -translate-y-1/2 w-1 rounded-r-full bg-white"
        animate={{ height: active ? 36 : badge > 0 ? 8 : 0 }}
        transition={{ type: "spring", stiffness: 300, damping: 25 }}
      />
      <Link href={href} title={label} className="perspective">
        <motion.div
          whileHover={{ rotateY: 12, rotateX: -8, scale: 1.06 }}
          whileTap={{ scale: 0.95 }}
          className={cn(
            "w-12 h-12 flex items-center justify-center transition-all duration-300 preserve-3d",
            active ? "rounded-2xl" : "rounded-3xl group-hover:rounded-2xl",
            !color && (active ? "bg-gradient-to-br from-cyan-400/30 to-violet-500/30 text-white glow-cyan" : "bg-white/[0.05] text-slate-300 group-hover:bg-cyan-400/20 group-hover:text-white")
          )}
          style={color ? {
            background: active ? `linear-gradient(135deg, ${color}, ${color}88)` : `${color}26`,
            color: active ? "#04050c" : color,
            boxShadow: active ? `0 0 22px -4px ${color}` : `inset 0 0 0 1px ${color}55`,
          } : undefined}
        >
          {children}
        </motion.div>
      </Link>
      {badge > 0 && <Badge count={badge} className="absolute -bottom-0.5 right-2.5 ring-2 ring-[#05060f]" />}
      <span className="pointer-events-none absolute left-[68px] top-1/2 -translate-y-1/2 z-50 whitespace-nowrap rounded-lg glass px-3 py-1.5 text-sm font-medium text-white opacity-0 -translate-x-1 transition-all group-hover:opacity-100 group-hover:translate-x-0">
        {label}
      </span>
    </div>
  );
}

function RailButton({ label, onClick, children }: { label: string; onClick: () => void; children: React.ReactNode }) {
  return (
    <div className="relative group w-full flex justify-center">
      <button
        onClick={onClick}
        title={label}
        className="w-12 h-12 rounded-3xl hover:rounded-2xl bg-white/[0.05] text-emerald-400 hover:bg-emerald-400 hover:text-slate-950 flex items-center justify-center transition-all duration-300 cursor-pointer"
      >
        {children}
      </button>
      <span className="pointer-events-none absolute left-[68px] top-1/2 -translate-y-1/2 z-50 whitespace-nowrap rounded-lg glass px-3 py-1.5 text-sm font-medium text-white opacity-0 transition-opacity group-hover:opacity-100">
        {label}
      </span>
    </div>
  );
}
