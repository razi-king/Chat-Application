"use client";
import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, Home, MessageCircle, Server, UserCircle } from "lucide-react";
import { useAppState } from "@/context/AppStateContext";
import { Badge } from "@/component/ui/Primitives";
import { cn } from "@/lib/utils";

// Inside An Open Chat The Composer Needs The Bottom Of The Screen (Like WhatsApp), So The Bar Hides
const CHAT_OPEN = [/^\/app\/chats\/[^/]+$/, /^\/app\/servers\/[^/]+\/[^/]+$/];

// Phones: The Discord Rail Becomes A Bottom Tab Bar (The Rail Would Eat ~20% Of A 375px Screen)
export default function MobileNav() {
  const pathname = usePathname();
  const { conversations, servers, notificationCount } = useAppState();
  if (CHAT_OPEN.some((r) => r.test(pathname))) return null;

  const tabs = [
    { href: "/app", label: "Home", icon: Home, active: pathname === "/app" || pathname.startsWith("/app/friends") || pathname.startsWith("/app/discover"), badge: 0 },
    { href: "/app/chats", label: "Chats", icon: MessageCircle, active: pathname.startsWith("/app/chats"), badge: conversations.reduce((s, r) => s + r.unreadCount, 0) },
    { href: "/app/servers", label: "Servers", icon: Server, active: pathname.startsWith("/app/servers"), badge: servers.reduce((s, r) => s + r.unreadCount, 0) },
    { href: "/app/notifications", label: "Alerts", icon: Bell, active: pathname.startsWith("/app/notifications"), badge: notificationCount },
    { href: "/app/profile", label: "Me", icon: UserCircle, active: pathname.startsWith("/app/profile"), badge: 0 },
  ];

  return (
    <nav className="md:hidden shrink-0 grid grid-cols-5 border-t border-line bg-[#05060f]/95 backdrop-blur-xl pb-[env(safe-area-inset-bottom)]">
      {tabs.map((t) => (
        <Link key={t.href} href={t.href} className={cn("relative flex flex-col items-center gap-0.5 py-2 text-[10px] font-medium transition-colors", t.active ? "text-cyan-300" : "text-slate-500")}>
          {t.active && <span className="absolute top-0 h-0.5 w-8 rounded-full bg-gradient-to-r from-cyan-400 to-violet-400" />}
          <t.icon size={21} />
          {t.label}
          {t.badge > 0 && <Badge count={t.badge} className="absolute top-1 left-1/2 ml-2 scale-90" />}
        </Link>
      ))}
    </nav>
  );
}
