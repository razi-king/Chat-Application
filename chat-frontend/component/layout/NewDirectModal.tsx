"use client";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Search } from "lucide-react";
import Modal from "@/component/ui/Modal";
import Avatar from "@/component/ui/Avatar";
import { Spinner } from "@/component/ui/Primitives";
import { UserService } from "@/services/AuthService";
import { RoomService } from "@/services/RoomService";
import { handleError } from "@/lib/errorHandler";
import { useAppState } from "@/context/AppStateContext";
import type { User } from "@/types";

// Search Anyone By Name / Username And Open A 1-to-1 Chat
export default function NewDirectModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const router = useRouter();
  const { refreshConversations, isOnline } = useAppState();
  const [q, setQ] = useState("");
  const [results, setResults] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    const term = q.trim();
    if (term.length < 1) return;
    const t = setTimeout(() => {
      setLoading(true);
      UserService.search(term)
        .then(setResults)
        .catch((e) => handleError(e))
        .finally(() => setLoading(false));
    }, 250);
    return () => clearTimeout(t);
  }, [q, open]);

  const shown = q.trim() ? results : [];

  const start = async (user: User) => {
    try {
      const room = await RoomService.openDirect(user.id);
      await refreshConversations();
      onClose();
      setQ("");
      router.push(`/app/chats/${room.id}`);
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="New chat" subtitle="Find someone by name or username">
      <div className="relative mb-4">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
        <input
          autoFocus
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search people..."
          className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-white/[0.04] border border-line focus:border-cyan-400/60 outline-none text-sm"
        />
      </div>
      <div className="min-h-32 max-h-72 overflow-y-auto space-y-1">
        {loading && <div className="flex justify-center py-6"><Spinner /></div>}
        {!loading && q && shown.length === 0 && <p className="text-center text-sm text-slate-500 py-6">No users found</p>}
        {!loading && !q && <p className="text-center text-sm text-slate-500 py-6">Try &quot;aisha&quot;, &quot;kabir&quot; or &quot;dev&quot;</p>}
        {shown.map((u) => (
          <button key={u.id} onClick={() => start(u)} className="w-full flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-white/5 transition-colors cursor-pointer text-left">
            <Avatar name={u.displayName} color={u.avatarColor} showStatus online={isOnline(u)} />
            <div className="min-w-0">
              <div className="text-sm font-semibold text-white">{u.displayName}</div>
              <div className="text-xs text-slate-500">@{u.username}</div>
            </div>
          </button>
        ))}
      </div>
    </Modal>
  );
}
