"use client";
import React, { useState } from "react";
import { motion } from "framer-motion";
import {
  AlertCircle, Check, CheckCheck, Clock, Copy, CornerUpLeft, Pencil, Pin, SmilePlus, Trash2,
} from "lucide-react";
import toast from "react-hot-toast";
import Avatar from "@/component/ui/Avatar";
import { cn, formatTime, QUICK_REACTIONS } from "@/lib/utils";
import type { Message } from "@/types";

export type ReadState = "pending" | "failed" | "sent" | "partial" | "read";

interface Props {
  message: Message;
  layout: "bubble" | "flat";
  mine: boolean;
  grouped: boolean;
  showSenderName: boolean;
  meId: string;
  readState?: ReadState;
  highlighted?: boolean;
  canModerate: boolean;
  canPin: boolean;
  onReply: (m: Message) => void;
  onReact: (m: Message, emoji: string) => void;
  onEdit: (m: Message, content: string) => Promise<void>;
  onDelete: (m: Message) => void;
  onPin: (m: Message) => void;
  onRetry: (m: Message) => void;
  onJumpTo: (messageId: string) => void;
}

// "@razi hi" -> @razi Rendered As A Glowing Chip
function renderContent(text: string) {
  const parts = text.split(/(@[a-zA-Z0-9_.]{3,20})/g);
  return parts.map((part, i) =>
    part.startsWith("@") ? (
      <span key={i} className="px-1 rounded bg-cyan-400/15 text-cyan-200 font-medium">{part}</span>
    ) : (
      <React.Fragment key={i}>{part}</React.Fragment>
    )
  );
}

function Ticks({ state }: { state?: ReadState }) {
  if (!state) return null;
  if (state === "pending") return <Clock size={12} className="text-white/60" />;
  if (state === "failed") return <AlertCircle size={13} className="text-rose-300" />;
  if (state === "sent") return <Check size={13} className="text-white/60" />;
  return <CheckCheck size={14} className={state === "read" ? "text-sky-300 drop-shadow-[0_0_4px_#7dd3fc]" : "text-white/60"} />;
}

export default function MessageItem(props: Props) {
  const { message: m, layout, mine, grouped, showSenderName, meId, readState, highlighted, canModerate, canPin } = props;
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(m.content);
  const [showReactions, setShowReactions] = useState(false);
  // Touch Screens Have No Hover, So A Tap On The Message Toggles The Action Toolbar
  const [toolsOpen, setToolsOpen] = useState(false);
  const toggleTools = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest("button, textarea, a")) return;
    setToolsOpen((o) => !o);
  };
  const mentionsMe = m.mentions?.includes(meId);

  // ---------- System Message ("Razi joined the server") ----------
  if (m.type === "SYSTEM") {
    return (
      <div className="flex justify-center my-3">
        <div className="glass-soft rounded-full px-4 py-1.5 text-xs text-slate-400 text-center max-w-md">
          {m.content} <span className="text-slate-600 ml-1">{formatTime(m.createdAt)}</span>
        </div>
      </div>
    );
  }

  const saveEdit = async () => {
    const text = draft.trim();
    if (!text || text === m.content) {
      setEditing(false);
      return;
    }
    await props.onEdit(m, text);
    setEditing(false);
  };

  const body = editing ? (
    <div className="w-full min-w-64">
      <textarea
        autoFocus
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            saveEdit();
          }
          if (e.key === "Escape") setEditing(false);
        }}
        rows={2}
        className="w-full rounded-lg bg-black/30 border border-cyan-400/40 px-3 py-2 text-sm outline-none resize-none text-white"
      />
      <div className="text-[11px] text-slate-400 mt-1">Enter to save · Esc to cancel</div>
    </div>
  ) : m.deleted ? (
    <span className="italic text-slate-400 text-sm">🚫 This message was deleted</span>
  ) : (
    <span className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">{renderContent(m.content)}</span>
  );

  const reply = m.replyTo && (
    <button
      onClick={() => props.onJumpTo(m.replyTo!.id)}
      className={cn(
        "block w-full text-left mb-1.5 rounded-lg border-l-2 px-2.5 py-1.5 text-xs cursor-pointer",
        mine && layout === "bubble" ? "bg-black/20 border-white/70" : "bg-white/[0.04] border-cyan-400"
      )}
    >
      <div className={cn("font-semibold", mine && layout === "bubble" ? "text-white" : "text-cyan-300")}>{m.replyTo.senderName}</div>
      <div className="truncate opacity-80">{m.replyTo.deleted ? "Deleted message" : m.replyTo.content}</div>
    </button>
  );

  const reactions = m.reactions?.length > 0 && (
    <div className={cn("flex flex-wrap gap-1 mt-1.5", mine && layout === "bubble" && "justify-end")}>
      {m.reactions.map((r) => {
        const reacted = r.userIds.includes(meId);
        return (
          <button
            key={r.emoji}
            onClick={() => props.onReact(m, r.emoji)}
            className={cn(
              "flex items-center gap-1 rounded-full px-2 py-0.5 text-xs border transition-all cursor-pointer animate-pop",
              reacted ? "bg-cyan-400/20 border-cyan-400/60 text-white" : "bg-white/[0.04] border-line text-slate-300 hover:border-white/30"
            )}
          >
            <span>{r.emoji}</span>
            <span className="font-semibold">{r.count}</span>
          </button>
        );
      })}
    </div>
  );

  // ---------- Hover Toolbar ----------
  const toolbar = !m.deleted && !m.pending && !editing && (
    <div
      className={cn(
        "absolute -top-4 z-10 group-hover:flex items-center gap-0.5 glass rounded-xl p-0.5 shadow-xl",
        toolsOpen ? "flex" : "hidden",
        mine && layout === "bubble" ? "right-2" : "right-4"
      )}
    >
      <div className="relative">
        <ToolButton title="React" onClick={() => setShowReactions((s) => !s)}><SmilePlus size={15} /></ToolButton>
        {showReactions && (
          <div className="absolute bottom-9 right-0 glass rounded-full px-2 py-1 flex gap-1" onMouseLeave={() => setShowReactions(false)}>
            {QUICK_REACTIONS.map((e) => (
              <button key={e} onClick={() => { props.onReact(m, e); setShowReactions(false); }} className="text-lg hover:scale-125 transition-transform cursor-pointer">{e}</button>
            ))}
          </div>
        )}
      </div>
      <ToolButton title="Reply" onClick={() => props.onReply(m)}><CornerUpLeft size={15} /></ToolButton>
      {mine && <ToolButton title="Edit" onClick={() => { setDraft(m.content); setEditing(true); }}><Pencil size={14} /></ToolButton>}
      {canPin && <ToolButton title={m.pinned ? "Unpin" : "Pin"} onClick={() => props.onPin(m)}><Pin size={14} className={m.pinned ? "text-amber-300" : ""} /></ToolButton>}
      <ToolButton title="Copy" onClick={() => { navigator.clipboard?.writeText(m.content); toast.success("Copied"); }}><Copy size={14} /></ToolButton>
      {(mine || canModerate) && <ToolButton title="Delete" danger onClick={() => props.onDelete(m)}><Trash2 size={14} /></ToolButton>}
    </div>
  );

  const meta = (
    <span className="inline-flex items-center gap-1 text-[10px] opacity-70 ml-2 align-bottom whitespace-nowrap">
      {m.pinned && <Pin size={10} className="text-amber-300" />}
      {m.edited && !m.deleted && "edited ·"}
      {formatTime(m.createdAt)}
      {mine && layout === "bubble" && <Ticks state={readState} />}
    </span>
  );

  // ---------- WhatsApp Bubble (DMs / Groups) ----------
  if (layout === "bubble") {
    return (
      <motion.div
        id={`msg-${m.id}`}
        initial={{ opacity: 0, y: 8, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.18 }}
        onClick={toggleTools}
        className={cn("group relative flex gap-2 px-3 sm:px-4", mine ? "justify-end" : "justify-start", grouped ? "mt-0.5" : "mt-3")}
      >
        {!mine && (
          <div className="w-8 shrink-0">
            {!grouped && showSenderName && <Avatar name={m.sender?.displayName} color={m.sender?.avatarColor} size="sm" />}
          </div>
        )}
        <div
          className={cn(
            "relative max-w-[78%] sm:max-w-[65%] px-3.5 py-2 shadow-lg transition-shadow",
            mine
              ? "rounded-2xl rounded-br-md bg-gradient-to-br from-cyan-500/85 via-sky-500/80 to-violet-500/85 text-white shadow-cyan-500/10"
              : "rounded-2xl rounded-bl-md glass text-slate-100",
            highlighted && "ring-2 ring-amber-300/80",
            m.pending && "opacity-70",
            mentionsMe && !mine && "ring-1 ring-amber-300/60"
          )}
        >
          {!mine && !grouped && showSenderName && (
            <div className="text-xs font-bold mb-0.5" style={{ color: m.sender?.avatarColor }}>{m.sender?.displayName}</div>
          )}
          {reply}
          {body}
          {meta}
          {readState === "failed" && (
            <button onClick={() => props.onRetry(m)} className="block text-[11px] text-rose-200 underline mt-1 cursor-pointer">Not sent · tap to retry</button>
          )}
          {reactions}
        </div>
        {toolbar}
      </motion.div>
    );
  }

  // ---------- Discord Flat Row (Server Channels) ----------
  return (
    <div
      id={`msg-${m.id}`}
      onClick={toggleTools}
      className={cn(
        "group relative flex gap-3 sm:gap-4 px-3 sm:px-4 py-0.5 hover:bg-white/[0.025] transition-colors",
        grouped ? "" : "mt-4 pt-1",
        mentionsMe && "bg-amber-400/[0.06] border-l-2 border-amber-300",
        highlighted && "bg-cyan-400/10"
      )}
    >
      <div className="w-10 shrink-0">
        {!grouped ? (
          <Avatar name={m.sender?.displayName} color={m.sender?.avatarColor} />
        ) : (
          <span className="hidden group-hover:block text-[10px] text-slate-500 mt-1.5 text-right">{formatTime(m.createdAt)}</span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        {!grouped && (
          <div className="flex items-baseline gap-2">
            <span className="font-semibold text-[15px]" style={{ color: m.sender?.avatarColor }}>{m.sender?.displayName}</span>
            <span className="text-[11px] text-slate-500">{formatTime(m.createdAt)}</span>
            {m.pinned && <Pin size={11} className="text-amber-300" />}
          </div>
        )}
        {reply}
        <div className={cn("text-slate-200", m.pending && "opacity-60")}>
          {body}
          {m.edited && !m.deleted && <span className="text-[10px] text-slate-500 ml-1">(edited)</span>}
          {readState === "failed" && (
            <button onClick={() => props.onRetry(m)} className="ml-2 text-[11px] text-rose-300 underline cursor-pointer">failed · retry</button>
          )}
        </div>
        {reactions}
      </div>
      {toolbar}
    </div>
  );
}

function ToolButton({ children, title, onClick, danger }: { children: React.ReactNode; title: string; onClick: () => void; danger?: boolean }) {
  return (
    <button
      title={title}
      onClick={onClick}
      className={cn("p-1.5 rounded-lg transition-colors cursor-pointer", danger ? "text-rose-300 hover:bg-rose-500/20" : "text-slate-300 hover:text-white hover:bg-white/10")}
    >
      {children}
    </button>
  );
}
