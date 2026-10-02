"use client";
import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, Compass, LayoutDashboard, UserCircle, Users } from "lucide-react";
import SidebarFrame from "@/component/layout/SidebarFrame";
import Avatar from "@/component/ui/Avatar";
import { Badge } from "@/component/ui/Primitives";
import { useAppState } from "@/context/AppStateContext";
import { cn, roomTitle } from "@/lib/utils";

const nav = [
  { href: "/app", label: "Dashboard", icon: LayoutDashboard },
  { href: "/app/friends", label: "Friends", icon: Users },
  { href: "/app/discover", label: "Discover", icon: Compass },
  { href: "/app/notifications", label: "Notifications", icon: Bell },
  { href: "/app/profile", label: "Profile", icon: UserCircle },
];

export default function HubLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { notificationCount, conversations, isOnline } = useAppState();
  return (
    <>
      <SidebarFrame className="hidden md:flex" header={<h2 className="font-display text-lg font-bold text-white">Home</h2>}>
        <div className="p-2 space-y-0.5">
          {nav.map((n) => {
            const active = pathname === n.href;
            return (
              <Link key={n.href} href={n.href}
                className={cn("flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-sm",
                  active ? "bg-gradient-to-r from-cyan-400/15 to-violet-400/10 text-white shadow-[inset_0_0_0_1px_rgba(34,211,238,0.2)]" : "text-slate-400 hover:text-white hover:bg-white/5")}>
                <n.icon size={18} className={active ? "text-cyan-300" : ""} />
                <span className="flex-1">{n.label}</span>
                {n.href === "/app/notifications" && <Badge count={notificationCount} />}
              </Link>
            );
          })}
        </div>
        <div className="px-4 mt-4 mb-2 text-[11px] font-semibold uppercase tracking-widest text-slate-500">Recent chats</div>
        <div className="px-2 space-y-0.5 pb-3">
          {conversations.slice(0, 8).map((r) => (
            <Link key={r.id} href={`/app/chats/${r.id}`} className="flex items-center gap-3 px-3 py-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-white/5">
              {r.type === "DIRECT"
                ? <Avatar name={r.otherUser?.displayName} color={r.otherUser?.avatarColor} size="sm" showStatus online={isOnline(r.otherUser)} />
                : <Avatar name={r.name} color={r.iconColor} size="sm" square />}
              <span className="truncate text-sm flex-1">{roomTitle(r)}</span>
              <Badge count={r.unreadCount} />
            </Link>
          ))}
        </div>
      </SidebarFrame>
      <main className="flex-1 min-w-0 overflow-y-auto">
        {/* Phones: Horizontal Tabs Instead Of The Sidebar */}
        <div className="md:hidden flex gap-1 overflow-x-auto p-2 border-b border-line">
          {nav.map((n) => (
            <Link key={n.href} href={n.href} className={cn("px-3 py-1.5 rounded-lg text-sm whitespace-nowrap", pathname === n.href ? "bg-cyan-400/15 text-white" : "text-slate-400")}>
              {n.label}
            </Link>
          ))}
        </div>
        {children}
      </main>
    </>
  );
}
