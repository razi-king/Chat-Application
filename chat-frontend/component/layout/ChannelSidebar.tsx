"use client";
import React, { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { ChevronDown, Hash, LogOut, Plus, ScrollText, Trash2, UserPlus } from "lucide-react";
import SidebarFrame from "./SidebarFrame";
import { AuditLogModal, CreateChannelModal, InviteModal } from "./ServerModals";
import { Badge } from "@/component/ui/Primitives";
import { ServerService } from "@/services/ServerService";
import { RoomService } from "@/services/RoomService";
import { handleError } from "@/lib/errorHandler";
import { useAppState } from "@/context/AppStateContext";
import { cn } from "@/lib/utils";
import type { ServerDetail } from "@/types";

// Discord Channel List With The Server Menu On Top
export default function ChannelSidebar({ detail, reload, className = "" }: { detail: ServerDetail; reload: () => Promise<void>; className?: string }) {
  const { channelId } = useParams<{ channelId?: string }>();
  const router = useRouter();
  const { refreshServers } = useAppState();
  const [menu, setMenu] = useState(false);
  const [modal, setModal] = useState<"channel" | "invite" | "audit" | null>(null);
  const { server, channels } = detail;
  const isAdmin = server.myRole === "OWNER" || server.myRole === "ADMIN";

  const leave = async () => {
    setMenu(false);
    if (!confirm(`Leave ${server.name}?`)) return;
    try {
      await ServerService.leave(server.id);
      toast.success(`You left ${server.name}`);
      await refreshServers();
      router.replace("/app");
    } catch (e) {
      handleError(e);
    }
  };

  const destroy = async () => {
    setMenu(false);
    if (!confirm(`Delete ${server.name} for everyone? This cannot be undone.`)) return;
    try {
      await ServerService.remove(server.id);
      toast.success("Server deleted");
      await refreshServers();
      router.replace("/app");
    } catch (e) {
      handleError(e);
    }
  };

  const deleteChannel = async (id: string, name?: string) => {
    if (!confirm(`Delete #${name}? All its messages will be lost.`)) return;
    try {
      await RoomService.deleteRoom(id);
      toast.success(`#${name} deleted`);
      await reload();
      if (id === channelId) router.replace(`/app/servers/${server.id}`);
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <SidebarFrame
      className={className}
      header={
        <button onClick={() => setMenu((m) => !m)} className="w-full flex items-center justify-between gap-2 cursor-pointer group">
          <div className="flex items-center gap-2 min-w-0">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: server.iconColor, boxShadow: `0 0 10px ${server.iconColor}` }} />
            <h2 className="font-display font-bold text-white truncate">{server.name}</h2>
          </div>
          <ChevronDown size={18} className={cn("text-slate-400 transition-transform", menu && "rotate-180")} />
        </button>
      }
    >
      {menu && (
        <div className="m-2 glass rounded-xl p-1.5 text-sm animate-pop">
          <MenuButton icon={<UserPlus size={15} />} label="Invite people" accent onClick={() => { setMenu(false); setModal("invite"); }} />
          {isAdmin && <MenuButton icon={<Plus size={15} />} label="Create channel" onClick={() => { setMenu(false); setModal("channel"); }} />}
          {isAdmin && <MenuButton icon={<ScrollText size={15} />} label="Audit log" onClick={() => { setMenu(false); setModal("audit"); }} />}
          <div className="h-px bg-line my-1" />
          {server.myRole === "OWNER" ? (
            <MenuButton icon={<Trash2 size={15} />} label="Delete server" danger onClick={destroy} />
          ) : (
            <MenuButton icon={<LogOut size={15} />} label="Leave server" danger onClick={leave} />
          )}
        </div>
      )}

      <div className="p-2">
        <div className="mx-2 my-3 rounded-xl p-3 text-xs text-slate-400 glass-soft">
          {server.description || "Welcome to the server!"}
          <div className="mt-1 text-slate-500">{server.memberCount} members · you are {server.myRole?.toLowerCase()}</div>
        </div>
        <div className="flex items-center justify-between px-2 mb-1 mt-4">
          <span className="text-[11px] font-semibold uppercase tracking-widest text-slate-500">Text channels</span>
          {isAdmin && (
            <button onClick={() => setModal("channel")} title="Create channel" className="text-slate-400 hover:text-white cursor-pointer"><Plus size={16} /></button>
          )}
        </div>
        {channels.map((c) => {
          const active = c.id === channelId;
          return (
            <div key={c.id} className="group relative">
              <Link
                href={`/app/servers/${server.id}/${c.id}`}
                className={cn(
                  "flex items-center gap-2 px-2.5 py-2 rounded-lg transition-all",
                  active ? "bg-gradient-to-r from-cyan-400/15 to-transparent text-white shadow-[inset_2px_0_0_#22d3ee]" : c.unreadCount > 0 ? "text-white hover:bg-white/5" : "text-slate-400 hover:text-slate-200 hover:bg-white/5"
                )}
              >
                <Hash size={17} className={active ? "text-cyan-300" : "text-slate-500"} />
                <span className={cn("truncate flex-1 text-sm", c.unreadCount > 0 && "font-semibold")}>{c.name}</span>
                <Badge count={c.unreadCount} />
              </Link>
              {isAdmin && channels.length > 1 && (
                <button onClick={() => deleteChannel(c.id, c.name)} title="Delete channel"
                  className="absolute right-2 top-1/2 -translate-y-1/2 hidden group-hover:block text-slate-500 hover:text-rose-300 cursor-pointer">
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          );
        })}
      </div>

      <CreateChannelModal serverId={server.id} open={modal === "channel"} onClose={() => setModal(null)}
        onCreated={async (room) => { setModal(null); await reload(); router.push(`/app/servers/${server.id}/${room.id}`); }} />
      <InviteModal serverId={server.id} serverName={server.name} open={modal === "invite"} onClose={() => setModal(null)} />
      <AuditLogModal serverId={server.id} open={modal === "audit"} onClose={() => setModal(null)} />
    </SidebarFrame>
  );
}

function MenuButton({ icon, label, onClick, danger, accent }: { icon: React.ReactNode; label: string; onClick: () => void; danger?: boolean; accent?: boolean }) {
  return (
    <button onClick={onClick}
      className={cn("w-full flex items-center justify-between px-3 py-2 rounded-lg cursor-pointer transition-colors",
        danger ? "text-rose-300 hover:bg-rose-500/15" : accent ? "text-cyan-300 hover:bg-cyan-400/10" : "text-slate-200 hover:bg-white/10")}>
      {label} {icon}
    </button>
  );
}
