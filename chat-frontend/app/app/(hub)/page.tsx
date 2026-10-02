"use client";
import React, { useEffect, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { Bell, Compass, MessageCircle, Server, UserPlus, Users, Wifi } from "lucide-react";
import { TiltCard } from "@/component/ui/Primitives";
import Avatar from "@/component/ui/Avatar";
import { UserService } from "@/services/AuthService";
import { useAuth } from "@/context/AuthContext";
import { useAppState } from "@/context/AppStateContext";
import { handleError } from "@/lib/errorHandler";
import type { Dashboard } from "@/types";

export default function DashboardPage() {
  const { user } = useAuth();
  const { servers, conversations, notificationCount } = useAppState();
  const [stats, setStats] = useState<Dashboard | null>(null);

  useEffect(() => {
    UserService.dashboard().then(setStats).catch((e) => handleError(e));
  }, [conversations, servers, notificationCount]);

  const hour = new Date().getHours();
  const greet = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const cards = [
    { label: "Unread messages", value: stats?.unreadMessages, icon: MessageCircle, color: "#22d3ee", href: "/app/chats" },
    { label: "Friends online", value: stats ? `${stats.onlineFriends}/${stats.friends}` : undefined, icon: Wifi, color: "#34d399", href: "/app/friends" },
    { label: "Servers", value: stats?.servers, icon: Server, color: "#a78bfa", href: "/app/discover" },
    { label: "Notifications", value: stats?.unreadNotifications, icon: Bell, color: "#f472b6", href: "/app/notifications" },
  ];

  return (
    <div className="p-6 lg:p-10 max-w-6xl">
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="flex items-center gap-5">
        <Avatar name={user?.displayName} color={user?.avatarColor} size="xl" />
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.25em] text-cyan-300">{greet}</p>
          <h1 className="font-display text-3xl lg:text-4xl font-bold text-white">{user?.displayName}</h1>
          <p className="text-slate-400 text-sm mt-1">{user?.customStatus || "Ready to connect."}</p>
        </div>
      </motion.div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-10">
        {cards.map((c, i) => (
          <motion.div key={c.label} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 * i }}>
            <Link href={c.href}>
              <TiltCard className="glass rounded-2xl p-5 h-full">
                <div className="flex items-center justify-between">
                  <span className="text-xs uppercase tracking-widest text-slate-500">{c.label}</span>
                  <c.icon size={18} color={c.color} />
                </div>
                <div className="font-display text-4xl font-bold text-white mt-4" style={{ textShadow: `0 0 24px ${c.color}66` }}>
                  {c.value ?? <span className="inline-block w-12 h-9 rounded skeleton" />}
                </div>
              </TiltCard>
            </Link>
          </motion.div>
        ))}
      </div>

      {stats && stats.pendingRequests > 0 && (
        <Link href="/app/friends" className="mt-6 flex items-center gap-3 glass rounded-2xl px-5 py-4 neon-border">
          <UserPlus className="text-cyan-300" />
          <span className="text-white">You have {stats.pendingRequests} pending friend request{stats.pendingRequests > 1 ? "s" : ""}</span>
          <span className="ml-auto text-sm text-cyan-300">Review →</span>
        </Link>
      )}

      <div className="grid lg:grid-cols-2 gap-6 mt-10">
        <section className="glass rounded-2xl p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-semibold text-white flex items-center gap-2"><Users size={18} className="text-emerald-300" /> Your servers</h2>
            <Link href="/app/discover" className="text-xs text-cyan-300 flex items-center gap-1"><Compass size={13} /> Discover</Link>
          </div>
          {servers.length === 0 && <p className="text-sm text-slate-500">No servers yet. Create one with the + button on the left.</p>}
          <div className="space-y-2">
            {servers.map((s) => (
              <Link key={s.id} href={`/app/servers/${s.id}`} className="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5">
                <Avatar name={s.name} color={s.iconColor} square />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-semibold text-white truncate">{s.name}</div>
                  <div className="text-xs text-slate-500">{s.memberCount} members · {s.myRole?.toLowerCase()}</div>
                </div>
                {s.unreadCount > 0 && <span className="text-xs text-cyan-300">{s.unreadCount} new</span>}
              </Link>
            ))}
          </div>
        </section>
        <section className="glass rounded-2xl p-5">
          <h2 className="font-display font-semibold text-white flex items-center gap-2 mb-4"><MessageCircle size={18} className="text-cyan-300" /> Latest conversations</h2>
          {conversations.length === 0 && <p className="text-sm text-slate-500">No chats yet. Open Chats and start one.</p>}
          <div className="space-y-2">
            {conversations.slice(0, 5).map((r) => (
              <Link key={r.id} href={`/app/chats/${r.id}`} className="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5">
                <Avatar name={r.type === "DIRECT" ? r.otherUser?.displayName : r.name} color={r.type === "DIRECT" ? r.otherUser?.avatarColor : r.iconColor} square={r.type !== "DIRECT"} />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-semibold text-white truncate">{r.type === "DIRECT" ? r.otherUser?.displayName : r.name}</div>
                  <div className="text-xs text-slate-500 truncate">{r.lastMessage?.preview ?? "No messages yet"}</div>
                </div>
                {r.unreadCount > 0 && <span className="text-xs text-cyan-300">{r.unreadCount}</span>}
              </Link>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
