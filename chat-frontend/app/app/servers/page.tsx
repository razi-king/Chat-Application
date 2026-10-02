"use client";
import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Compass, LogIn, Plus, Server as ServerIcon } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import Modal from "@/component/ui/Modal";
import { Badge, EmptyState } from "@/component/ui/Primitives";
import CreateRoomChat from "@/component/chat/CreateRoomChat";
import JoinRoomChat from "@/component/chat/JoinRoomChat";
import { useAppState } from "@/context/AppStateContext";

// The "Servers" Tab On Phones (On Desktop The Same List Lives In The Left Rail)
export default function ServersPage() {
  const router = useRouter();
  const { servers, loaded, refreshServers, refreshConversations } = useAppState();
  const [modal, setModal] = useState<"create" | "join" | null>(null);

  const done = (path: string) => {
    setModal(null);
    refreshServers();
    refreshConversations();
    router.push(path);
  };

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-8 max-w-3xl w-full mx-auto">
      <h1 className="font-display text-2xl sm:text-3xl font-bold text-white flex items-center gap-3">
        <ServerIcon className="text-violet-300" /> Servers
      </h1>

      <div className="grid grid-cols-3 gap-2 sm:gap-3 mt-5">
        <QuickAction icon={<Plus size={20} />} label="Create" onClick={() => setModal("create")} color="text-emerald-300" />
        <QuickAction icon={<LogIn size={20} />} label="Join" onClick={() => setModal("join")} color="text-cyan-300" />
        <Link href="/app/discover" className="glass rounded-2xl py-4 flex flex-col items-center gap-1.5 text-sm text-slate-200 active:scale-95 transition-transform">
          <Compass size={20} className="text-violet-300" /> Discover
        </Link>
      </div>

      <div className="mt-6 space-y-2">
        {!loaded && Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-16 rounded-2xl skeleton" />)}
        {loaded && servers.length === 0 && (
          <EmptyState icon={<ServerIcon size={26} />} title="No servers yet" text="Create one for your class or join with an invite code." />
        )}
        {servers.map((s) => (
          <Link key={s.id} href={`/app/servers/${s.id}`} className="glass-soft rounded-2xl p-3 flex items-center gap-3 active:bg-white/5 hover:border-cyan-400/30 transition-colors">
            <Avatar name={s.name} color={s.iconColor} size="lg" square />
            <div className="min-w-0 flex-1">
              <div className="font-semibold text-white truncate">{s.name}</div>
              <div className="text-xs text-slate-500 truncate">{s.memberCount} members · {s.myRole?.toLowerCase()}</div>
            </div>
            <Badge count={s.unreadCount} />
          </Link>
        ))}
      </div>

      <Modal open={modal === "create"} onClose={() => setModal(null)} title="Create" subtitle="A server for your community, or a group for your friends">
        <CreateRoomChat onDone={done} />
      </Modal>
      <Modal open={modal === "join"} onClose={() => setModal(null)} title="Join a server" subtitle="Paste an invite code or link">
        <JoinRoomChat onDone={done} />
      </Modal>
    </div>
  );
}

function QuickAction({ icon, label, onClick, color }: { icon: React.ReactNode; label: string; onClick: () => void; color: string }) {
  return (
    <button onClick={onClick} className="glass rounded-2xl py-4 flex flex-col items-center gap-1.5 text-sm text-slate-200 active:scale-95 transition-transform cursor-pointer">
      <span className={color}>{icon}</span> {label}
    </button>
  );
}
