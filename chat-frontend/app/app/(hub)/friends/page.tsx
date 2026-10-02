"use client";
import React, { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { Check, MessageCircle, UserMinus, UserPlus, Users, X } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { EmptyState, Spinner } from "@/component/ui/Primitives";
import ReusableForm from "@/component/form/ReusableForm";
import { FormStyles } from "@/component/enums/FormStyles";
import { addFriendSchema } from "@/component/form/formSchema";
import { FriendService } from "@/services/SocialService";
import { RoomService } from "@/services/RoomService";
import { useAppState } from "@/context/AppStateContext";
import { useSocket } from "@/context/SocketContext";
import { handleError } from "@/lib/errorHandler";
import { cn, lastSeenText } from "@/lib/utils";
import type { Friendship } from "@/types";

type Tab = "online" | "all" | "pending" | "add";

export default function FriendsPage() {
  const router = useRouter();
  const { isOnline } = useAppState();
  const { onEvent } = useSocket();
  const [items, setItems] = useState<Friendship[] | null>(null);
  const [tab, setTab] = useState<Tab>("online");

  const load = useCallback(() => {
    FriendService.list().then(setItems).catch((e) => { handleError(e); setItems([]); });
  }, []);

  useEffect(() => load(), [load]);
  // A Friend Request / Acceptance Notification Arrived -> Refresh The List
  useEffect(() => onEvent((e) => { if (e.type === "NOTIFICATION") load(); }), [onEvent, load]);

  const friends = items?.filter((f) => f.status === "ACCEPTED") ?? [];
  const pending = items?.filter((f) => f.status === "PENDING") ?? [];
  const shown = tab === "online" ? friends.filter((f) => isOnline(f.user)) : tab === "all" ? friends : pending;

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      await fn();
      toast.success(ok);
      load();
    } catch (e) {
      handleError(e);
    }
  };

  const message = async (userId: string) => {
    try {
      const room = await RoomService.openDirect(userId);
      router.push(`/app/chats/${room.id}`);
    } catch (e) {
      handleError(e);
    }
  };

  const tabs: { id: Tab; label: string; count?: number }[] = [
    { id: "online", label: "Online", count: friends.filter((f) => isOnline(f.user)).length },
    { id: "all", label: "All", count: friends.length },
    { id: "pending", label: "Pending", count: pending.length },
    { id: "add", label: "Add friend" },
  ];

  return (
    <div className="p-6 lg:p-10 max-w-4xl">
      <h1 className="font-display text-3xl font-bold text-white flex items-center gap-3"><Users className="text-cyan-300" /> Friends</h1>
      <div className="flex flex-wrap gap-2 mt-6">
        {tabs.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={cn("px-4 py-1.5 rounded-xl text-sm font-medium transition-all cursor-pointer",
              t.id === "add" ? (tab === "add" ? "bg-emerald-400 text-slate-950" : "bg-emerald-400/15 text-emerald-300 hover:bg-emerald-400/25")
                : tab === t.id ? "bg-white/10 text-white" : "text-slate-400 hover:text-white hover:bg-white/5")}>
            {t.label}{t.count !== undefined && <span className="ml-1.5 opacity-60">{t.count}</span>}
          </button>
        ))}
      </div>

      {tab === "add" ? (
        <div className="glass rounded-2xl p-6 mt-6 max-w-lg">
          <ReusableForm
            fullScreen={false}
            formTitle=""
            formStyle={FormStyles.MODALFORM}
            initialValues={{ username: "" }}
            validationSchema={addFriendSchema}
            buttonText="Send friend request"
            fields={[{ name: "username", label: "Username", placeHolder: "e.g. kabir" }]}
            onSubmit={async (values, actions) => {
              try {
                await FriendService.sendRequest(values.username);
                toast.success(`Request sent to @${values.username}`);
                actions.resetForm();
                load();
              } catch (e) {
                handleError(e, { setFieldErrors: actions.setErrors });
                actions.setFieldError("username", "Could not send request");
              } finally {
                actions.setSubmitting(false);
              }
            }}
          />
        </div>
      ) : (
        <div className="mt-6 space-y-2">
          {items === null && <div className="flex justify-center py-10"><Spinner size={28} /></div>}
          {items && shown.length === 0 && (
            <EmptyState icon={<UserPlus size={26} />} title={tab === "pending" ? "No pending requests" : tab === "online" ? "Nobody is online right now" : "No friends yet"}
              text="Add people by their username to see them here." />
          )}
          {shown.map((f) => (
            <div key={f.id} className="glass-soft rounded-2xl px-4 py-3 flex items-center gap-4 hover:border-cyan-400/30 transition-colors">
              <Avatar name={f.user.displayName} color={f.user.avatarColor} showStatus online={isOnline(f.user)} />
              <div className="min-w-0 flex-1">
                <div className="text-sm font-semibold text-white">{f.user.displayName} <span className="text-slate-500 font-normal">@{f.user.username}</span></div>
                <div className="text-xs text-slate-500">
                  {f.status === "PENDING" ? (f.incoming ? "Incoming friend request" : "Outgoing friend request") : f.user.customStatus || lastSeenText(f.user, isOnline(f.user))}
                </div>
              </div>
              {f.status === "ACCEPTED" && (
                <>
                  <IconAction title="Message" onClick={() => message(f.user.id)}><MessageCircle size={17} /></IconAction>
                  <IconAction title="Remove friend" danger onClick={() => confirm(`Remove ${f.user.displayName}?`) && run(() => FriendService.remove(f.id), "Friend removed")}><UserMinus size={17} /></IconAction>
                </>
              )}
              {f.status === "PENDING" && f.incoming && (
                <IconAction title="Accept" accent onClick={() => run(() => FriendService.accept(f.id), `You and ${f.user.displayName} are now friends`)}><Check size={17} /></IconAction>
              )}
              {f.status === "PENDING" && (
                <IconAction title={f.incoming ? "Decline" : "Cancel"} danger onClick={() => run(() => FriendService.remove(f.id), f.incoming ? "Request declined" : "Request cancelled")}><X size={17} /></IconAction>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function IconAction({ children, title, onClick, danger, accent }: { children: React.ReactNode; title: string; onClick: () => void; danger?: boolean; accent?: boolean }) {
  return (
    <button title={title} onClick={onClick}
      className={cn("w-9 h-9 rounded-full flex items-center justify-center transition-colors cursor-pointer",
        danger ? "bg-white/5 text-slate-400 hover:text-rose-300 hover:bg-rose-500/15" : accent ? "bg-emerald-400/15 text-emerald-300 hover:bg-emerald-400/30" : "bg-white/5 text-slate-300 hover:text-cyan-300 hover:bg-cyan-400/15")}>
      {children}
    </button>
  );
}
