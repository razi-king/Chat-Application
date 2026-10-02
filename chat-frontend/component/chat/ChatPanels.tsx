"use client";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { Crown, LogOut, MessageCircle, Pin, Search, Shield, Trash2, UserMinus, UserPlus, X } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import Modal from "@/component/ui/Modal";
import { RoleBadge, Spinner } from "@/component/ui/Primitives";
import { RoomService } from "@/services/RoomService";
import { ServerService } from "@/services/ServerService";
import { FriendService } from "@/services/SocialService";
import { handleError } from "@/lib/errorHandler";
import { cn, formatDay, formatTime, lastSeenText } from "@/lib/utils";
import { useAppState } from "@/context/AppStateContext";
import { useAuth } from "@/context/AuthContext";
import type { MemberRole, Message, Room, ServerDetail, ServerMember, User } from "@/types";

export function PanelFrame({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <aside className="w-full sm:w-80 shrink-0 border-l border-line bg-surface/80 backdrop-blur-xl flex flex-col absolute sm:static inset-0 z-30">
      <div className="h-16 shrink-0 flex items-center justify-between px-4 border-b border-line">
        <h3 className="font-display font-semibold text-white">{title}</h3>
        <button onClick={onClose} className="text-slate-400 hover:text-white cursor-pointer"><X size={18} /></button>
      </div>
      <div className="flex-1 overflow-y-auto">{children}</div>
    </aside>
  );
}

// ---------- Discord Member List (Grouped By Role, Online First) ----------
export function MemberListPanel({ detail, onClose, reload }: { detail: ServerDetail; onClose: () => void; reload: () => Promise<void> }) {
  const { isOnline } = useAppState();
  const { user } = useAuth();
  const router = useRouter();
  const [menuFor, setMenuFor] = useState<string | null>(null);
  const myRole = detail.server.myRole;
  const groups: { label: string; items: ServerMember[] }[] = [
    { label: "Owner", items: detail.members.filter((m) => m.role === "OWNER") },
    { label: "Admins", items: detail.members.filter((m) => m.role === "ADMIN") },
    { label: "Online", items: detail.members.filter((m) => m.role === "MEMBER" && isOnline(m.user)) },
    { label: "Offline", items: detail.members.filter((m) => m.role === "MEMBER" && !isOnline(m.user)) },
  ];

  const act = async (fn: () => Promise<unknown>, ok: string) => {
    setMenuFor(null);
    try {
      await fn();
      toast.success(ok);
      await reload();
    } catch (e) {
      handleError(e);
    }
  };

  const canManage = (m: ServerMember) =>
    m.user.id !== user?.id && m.role !== "OWNER" && (myRole === "OWNER" || (myRole === "ADMIN" && m.role === "MEMBER"));

  return (
    <PanelFrame title={`Members — ${detail.members.length}`} onClose={onClose}>
      <div className="p-3 space-y-4">
        {groups.filter((g) => g.items.length > 0).map((g) => (
          <div key={g.label}>
            <div className="text-[11px] uppercase tracking-widest text-slate-500 px-2 mb-1">{g.label} — {g.items.length}</div>
            {g.items.map((m) => (
              <div key={m.user.id} className="relative">
                <button
                  onClick={() => setMenuFor(menuFor === m.user.id ? null : m.user.id)}
                  className={cn("w-full flex items-center gap-3 px-2 py-1.5 rounded-xl hover:bg-white/5 transition-colors cursor-pointer text-left", !isOnline(m.user) && "opacity-50")}
                >
                  <Avatar name={m.user.displayName} color={m.user.avatarColor} size="sm" showStatus online={isOnline(m.user)} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 text-sm font-medium" style={{ color: m.role !== "MEMBER" ? m.user.avatarColor : undefined }}>
                      <span className="truncate">{m.user.displayName}</span>
                      {m.role === "OWNER" && <Crown size={12} className="text-amber-300 shrink-0" />}
                    </div>
                    {m.user.customStatus && <div className="text-[11px] text-slate-500 truncate">{m.user.customStatus}</div>}
                  </div>
                </button>
                {menuFor === m.user.id && (
                  <div className="mx-2 mb-2 glass rounded-xl p-1 text-sm animate-pop">
                    {m.user.id !== user?.id && (
                      <MenuItem icon={<MessageCircle size={14} />} label="Message" onClick={async () => {
                        try {
                          const room = await RoomService.openDirect(m.user.id);
                          router.push(`/app/chats/${room.id}`);
                        } catch (e) { handleError(e); }
                      }} />
                    )}
                    {m.user.id !== user?.id && (
                      <MenuItem icon={<UserPlus size={14} />} label="Add friend" onClick={() => act(() => FriendService.sendRequest(m.user.username), "Friend request sent")} />
                    )}
                    {myRole === "OWNER" && m.role !== "OWNER" && (
                      <MenuItem
                        icon={<Shield size={14} />}
                        label={m.role === "ADMIN" ? "Remove admin" : "Make admin"}
                        onClick={() => act(() => ServerService.updateRole(detail.server.id, m.user.id, (m.role === "ADMIN" ? "MEMBER" : "ADMIN") as MemberRole), "Role updated")}
                      />
                    )}
                    {canManage(m) && (
                      <MenuItem danger icon={<UserMinus size={14} />} label="Kick from server" onClick={() => act(() => ServerService.kick(detail.server.id, m.user.id), `${m.user.displayName} was kicked`)} />
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        ))}
      </div>
    </PanelFrame>
  );
}

function MenuItem({ icon, label, onClick, danger }: { icon: React.ReactNode; label: string; onClick: () => void; danger?: boolean }) {
  return (
    <button onClick={onClick} className={cn("w-full flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer", danger ? "text-rose-300 hover:bg-rose-500/15" : "text-slate-200 hover:bg-white/10")}>
      {icon} {label}
    </button>
  );
}

// ---------- WhatsApp Contact Info / Group Info ----------
export function RoomInfoPanel({ room, onClose, onChanged }: { room: Room; onClose: () => void; onChanged: () => void }) {
  const { isOnline, refreshConversations } = useAppState();
  const { user } = useAuth();
  const router = useRouter();
  const [adding, setAdding] = useState(false);
  const isAdmin = room.myRole === "OWNER" || room.myRole === "ADMIN";

  if (room.type === "DIRECT" && room.otherUser) {
    const u = room.otherUser;
    return (
      <PanelFrame title="Contact info" onClose={onClose}>
        <div className="p-6 text-center">
          <div className="flex justify-center"><Avatar name={u.displayName} color={u.avatarColor} size="xl" showStatus online={isOnline(u)} /></div>
          <h2 className="font-display text-xl font-bold text-white mt-4">{u.displayName}</h2>
          <p className="text-sm text-slate-500">@{u.username}</p>
          <p className={cn("text-xs mt-1", isOnline(u) ? "text-emerald-300" : "text-slate-500")}>{lastSeenText(u, isOnline(u))}</p>
          {u.customStatus && <p className="mt-4 glass-soft rounded-xl px-3 py-2 text-sm text-slate-300">{u.customStatus}</p>}
        </div>
        <div className="px-6 pb-6 space-y-4">
          <InfoRow label="About" value={u.about || "—"} />
          {u.createdAt && <InfoRow label="Member since" value={formatDay(u.createdAt)} />}
        </div>
      </PanelFrame>
    );
  }

  const leave = async () => {
    if (!user || !confirm("Leave this group?")) return;
    try {
      await RoomService.removeMember(room.id, user.id);
      toast.success("You left the group");
      await refreshConversations();
      router.replace("/app/chats");
    } catch (e) {
      handleError(e);
    }
  };

  const remove = async () => {
    if (!confirm("Delete this group for everyone?")) return;
    try {
      await RoomService.deleteRoom(room.id);
      toast.success("Group deleted");
      await refreshConversations();
      router.replace("/app/chats");
    } catch (e) {
      handleError(e);
    }
  };

  const kick = async (target: User) => {
    try {
      await RoomService.removeMember(room.id, target.id);
      toast.success(`${target.displayName} removed`);
      onChanged();
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <PanelFrame title="Group info" onClose={onClose}>
      <div className="p-6 text-center border-b border-line">
        <div className="flex justify-center"><Avatar name={room.name} color={room.iconColor} size="xl" square /></div>
        <h2 className="font-display text-xl font-bold text-white mt-4">{room.name}</h2>
        <p className="text-sm text-slate-500">Group · {room.members?.length ?? 0} members</p>
        {room.description && <p className="text-sm text-slate-300 mt-3">{room.description}</p>}
      </div>
      <div className="p-3">
        <div className="flex items-center justify-between px-2 mb-2">
          <span className="text-[11px] uppercase tracking-widest text-slate-500">Members</span>
          {isAdmin && (
            <button onClick={() => setAdding(true)} className="text-xs text-cyan-300 hover:text-cyan-200 flex items-center gap-1 cursor-pointer">
              <UserPlus size={13} /> Add
            </button>
          )}
        </div>
        {room.members?.map((m) => (
          <div key={m.user.id} className="flex items-center gap-3 px-2 py-1.5 rounded-xl hover:bg-white/5 group">
            <Avatar name={m.user.displayName} color={m.user.avatarColor} size="sm" showStatus online={isOnline(m.user)} />
            <div className="min-w-0 flex-1">
              <div className="text-sm text-white truncate">{m.user.id === user?.id ? "You" : m.user.displayName}</div>
              <div className="text-[11px] text-slate-500">{lastSeenText(m.user, isOnline(m.user))}</div>
            </div>
            <RoleBadge role={m.role} />
            {isAdmin && m.user.id !== user?.id && m.role !== "OWNER" && (
              <button onClick={() => kick(m.user)} title="Remove" className="hidden group-hover:block text-rose-300 hover:text-rose-200 cursor-pointer">
                <UserMinus size={15} />
              </button>
            )}
          </div>
        ))}
      </div>
      <div className="p-3 space-y-2 border-t border-line">
        <button onClick={leave} className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-rose-300 hover:bg-rose-500/10 cursor-pointer">
          <LogOut size={16} /> Leave group
        </button>
        {room.myRole === "OWNER" && (
          <button onClick={remove} className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-rose-300 hover:bg-rose-500/10 cursor-pointer">
            <Trash2 size={16} /> Delete group
          </button>
        )}
      </div>
      <AddMembersModal open={adding} room={room} onClose={() => setAdding(false)} onAdded={onChanged} />
    </PanelFrame>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-left">
      <div className="text-[11px] uppercase tracking-widest text-slate-500">{label}</div>
      <div className="text-sm text-slate-200 mt-1">{value}</div>
    </div>
  );
}

function AddMembersModal({ open, room, onClose, onAdded }: { open: boolean; room: Room; onClose: () => void; onAdded: () => void }) {
  const [friends, setFriends] = useState<User[]>([]);
  useEffect(() => {
    if (!open) return;
    const memberIds = new Set(room.members?.map((m) => m.user.id));
    FriendService.list()
      .then((l) => setFriends(l.filter((f) => f.status === "ACCEPTED" && !memberIds.has(f.user.id)).map((f) => f.user)))
      .catch((e) => handleError(e));
  }, [open, room.members]);

  const add = async (u: User) => {
    try {
      await RoomService.addMembers(room.id, [u.id]);
      toast.success(`${u.displayName} added`);
      setFriends((f) => f.filter((x) => x.id !== u.id));
      onAdded();
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Add members" subtitle="Only friends can be added">
      {friends.length === 0 && <p className="text-sm text-slate-500 text-center py-6">All your friends are already here</p>}
      {friends.map((u) => (
        <div key={u.id} className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-white/5">
          <Avatar name={u.displayName} color={u.avatarColor} size="sm" />
          <span className="flex-1 text-sm text-white">{u.displayName}</span>
          <button onClick={() => add(u)} className="text-xs px-3 py-1 rounded-lg bg-cyan-400/15 text-cyan-200 hover:bg-cyan-400/25 cursor-pointer">Add</button>
        </div>
      ))}
    </Modal>
  );
}

// ---------- Pinned Messages ----------
export function PinnedPanel({ roomId, onClose, onJump, version }: { roomId: string; onClose: () => void; onJump: (id: string) => void; version: number }) {
  const [items, setItems] = useState<Message[] | null>(null);
  useEffect(() => {
    RoomService.pinned(roomId).then(setItems).catch((e) => { handleError(e); setItems([]); });
  }, [roomId, version]);
  return (
    <PanelFrame title="Pinned messages" onClose={onClose}>
      <div className="p-3 space-y-2">
        {items === null && <div className="flex justify-center py-8"><Spinner /></div>}
        {items?.length === 0 && (
          <div className="text-center py-10 text-slate-500 text-sm"><Pin className="mx-auto mb-2 opacity-50" />No pinned messages yet</div>
        )}
        {items?.map((m) => <ResultCard key={m.id} m={m} onClick={() => onJump(m.id)} />)}
      </div>
    </PanelFrame>
  );
}

// ---------- Search In Room ----------
export function SearchPanel({ roomId, onClose, onJump }: { roomId: string; onClose: () => void; onJump: (id: string) => void }) {
  const [q, setQ] = useState("");
  const [items, setItems] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (!q.trim()) return;
    const t = setTimeout(() => {
      setLoading(true);
      RoomService.search(roomId, q).then(setItems).catch((e) => handleError(e)).finally(() => setLoading(false));
    }, 300);
    return () => clearTimeout(t);
  }, [q, roomId]);
  return (
    <PanelFrame title="Search" onClose={onClose}>
      <div className="p-3">
        <div className="relative mb-3">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search messages"
            className="w-full pl-9 pr-3 py-2 rounded-xl bg-white/[0.04] border border-line focus:border-cyan-400/50 outline-none text-sm" />
        </div>
        {loading && <div className="flex justify-center py-6"><Spinner /></div>}
        {!loading && q.trim() && items.length === 0 && <p className="text-center text-sm text-slate-500 py-6">No results</p>}
        <div className="space-y-2">{(q.trim() ? items : []).map((m) => <ResultCard key={m.id} m={m} onClick={() => onJump(m.id)} />)}</div>
      </div>
    </PanelFrame>
  );
}

function ResultCard({ m, onClick }: { m: Message; onClick: () => void }) {
  return (
    <button onClick={onClick} className="w-full text-left glass-soft rounded-xl p-3 hover:border-cyan-400/40 transition-colors cursor-pointer">
      <div className="flex items-center gap-2">
        <Avatar name={m.sender?.displayName} color={m.sender?.avatarColor} size="xs" />
        <span className="text-xs font-semibold text-white">{m.sender?.displayName}</span>
        <span className="text-[10px] text-slate-500 ml-auto">{formatDay(m.createdAt)} {formatTime(m.createdAt)}</span>
      </div>
      <p className="text-sm text-slate-300 mt-1.5 line-clamp-3">{m.content}</p>
    </button>
  );
}
