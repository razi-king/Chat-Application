"use client";
import React from "react";
import Link from "next/link";
import { LogOut, Settings } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { useAuth } from "@/context/AuthContext";
import { useSocket } from "@/context/SocketContext";

// Bottom Of Every Sidebar: Who Am I + Settings + Logout
export default function UserPanel() {
  const { user, logout } = useAuth();
  const { connected } = useSocket();
  if (!user) return null;
  return (
    <div className="hidden md:block mt-auto p-2 border-t border-line bg-black/20">
      <div className="flex items-center gap-2 rounded-xl px-2 py-1.5 hover:bg-white/5 transition-colors">
        <Avatar name={user.displayName} color={user.avatarColor} size="sm" showStatus online={connected} />
        <div className="min-w-0 flex-1">
          <div className="text-sm font-semibold text-white truncate">{user.displayName}</div>
          <div className="text-[11px] text-slate-500 truncate">{user.customStatus || `@${user.username}`}</div>
        </div>
        <Link href="/app/profile" title="Profile settings" className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10">
          <Settings size={16} />
        </Link>
        <button onClick={logout} title="Log out" className="p-1.5 rounded-lg text-slate-400 hover:text-rose-300 hover:bg-rose-500/10 cursor-pointer">
          <LogOut size={16} />
        </button>
      </div>
    </div>
  );
}
