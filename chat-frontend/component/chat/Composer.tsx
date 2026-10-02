"use client";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { MdSend } from "react-icons/md";
import { Smile, X } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { cn, EMOJIS } from "@/lib/utils";
import type { Message, User } from "@/types";

const MAX = 2000;

interface Props {
  placeholder: string;
  replyTo: Message | null;
  onCancelReply: () => void;
  onSend: (content: string) => void;
  onTyping: (typing: boolean) => void;
  mentionables: User[];
  disabled?: boolean;
}

export default function Composer({ placeholder, replyTo, onCancelReply, onSend, onTyping, mentionables, disabled }: Props) {
  const [text, setText] = useState("");
  const [showEmoji, setShowEmoji] = useState(false);
  const [mentionIndex, setMentionIndex] = useState(0);
  const ref = useRef<HTMLTextAreaElement>(null);
  const typingSent = useRef(0);
  const stopTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Auto-Grow The Textarea Up To ~6 Lines
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    // Empty -> Natural One-Line Height. Measuring scrollHeight Here Would Include The Placeholder,
    // Which Wraps Into Many Lines If The Layout Is Still Narrow During The First Render
    if (!text) {
      el.style.height = "";
      return;
    }
    el.style.height = "auto";
    el.style.height = Math.min(el.scrollHeight, 160) + "px";
  }, [text]);

  useEffect(() => {
    if (replyTo) ref.current?.focus();
  }, [replyTo]);

  useEffect(() => () => {
    if (stopTimer.current) clearTimeout(stopTimer.current);
  }, []);

  // "@ka" At The End Of The Text -> Suggest Kabir
  const mentionQuery = useMemo(() => {
    const match = /(^|\s)@([a-zA-Z0-9_.]*)$/.exec(text);
    return match ? match[2].toLowerCase() : null;
  }, [text]);
  const suggestions = useMemo(
    () =>
      mentionQuery === null
        ? []
        : mentionables
            .filter((u) => u.username.includes(mentionQuery) || u.displayName.toLowerCase().includes(mentionQuery))
            .slice(0, 6),
    [mentionQuery, mentionables]
  );

  const signalTyping = () => {
    const now = Date.now();
    if (now - typingSent.current > 2500) {
      typingSent.current = now;
      onTyping(true);
    }
    if (stopTimer.current) clearTimeout(stopTimer.current);
    stopTimer.current = setTimeout(() => {
      typingSent.current = 0;
      onTyping(false);
    }, 3000);
  };

  const insertMention = (u: User) => {
    setText((t) => t.replace(/@([a-zA-Z0-9_.]*)$/, `@${u.username} `));
    setMentionIndex(0);
    ref.current?.focus();
  };

  const submit = () => {
    const content = text.trim();
    if (!content || disabled) return;
    onSend(content);
    setText("");
    setShowEmoji(false);
    if (stopTimer.current) clearTimeout(stopTimer.current);
    typingSent.current = 0;
    onTyping(false);
  };

  return (
    <div className="px-4 pb-4 pt-2 relative">
      <AnimatePresence>
        {replyTo && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="glass rounded-t-2xl border-b-0 px-4 py-2 flex items-center gap-3 text-sm"
          >
            <div className="w-1 self-stretch rounded-full bg-cyan-400" />
            <div className="min-w-0 flex-1">
              <div className="text-xs text-cyan-300 font-semibold">Replying to {replyTo.sender?.displayName}</div>
              <div className="truncate text-slate-400">{replyTo.content}</div>
            </div>
            <button onClick={onCancelReply} className="text-slate-400 hover:text-white cursor-pointer"><X size={16} /></button>
          </motion.div>
        )}
      </AnimatePresence>

      {suggestions.length > 0 && (
        <div className="absolute bottom-full left-4 right-4 mb-2 glass rounded-xl p-1 z-20">
          <div className="text-[10px] uppercase tracking-widest text-slate-500 px-2 py-1">Mention</div>
          {suggestions.map((u, i) => (
            <button
              key={u.id}
              onMouseDown={(e) => { e.preventDefault(); insertMention(u); }}
              className={cn("w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm cursor-pointer", i === mentionIndex ? "bg-cyan-400/15" : "hover:bg-white/5")}
            >
              <Avatar name={u.displayName} color={u.avatarColor} size="xs" />
              <span className="text-white">{u.displayName}</span>
              <span className="text-slate-500">@{u.username}</span>
            </button>
          ))}
        </div>
      )}

      {showEmoji && (
        <div className="absolute bottom-full right-4 mb-2 glass rounded-2xl p-3 z-20 w-72 grid grid-cols-8 gap-1">
          {EMOJIS.map((e) => (
            <button key={e} onClick={() => { setText((t) => t + e); ref.current?.focus(); }} className="text-xl hover:scale-125 transition-transform cursor-pointer">
              {e}
            </button>
          ))}
        </div>
      )}

      <div className={cn("glass flex items-end gap-2 px-3 py-2 transition-shadow focus-within:shadow-[0_0_0_1px_rgba(34,211,238,0.45),0_0_30px_-10px_rgba(34,211,238,0.6)]", replyTo ? "rounded-b-2xl" : "rounded-2xl")}>
        <textarea
          ref={ref}
          value={text}
          disabled={disabled}
          maxLength={MAX}
          rows={1}
          placeholder={placeholder}
          onChange={(e) => {
            setText(e.target.value);
            signalTyping();
          }}
          onKeyDown={(e) => {
            if (suggestions.length > 0) {
              if (e.key === "ArrowDown") { e.preventDefault(); setMentionIndex((i) => (i + 1) % suggestions.length); return; }
              if (e.key === "ArrowUp") { e.preventDefault(); setMentionIndex((i) => (i - 1 + suggestions.length) % suggestions.length); return; }
              if (e.key === "Enter" || e.key === "Tab") { e.preventDefault(); insertMention(suggestions[mentionIndex] ?? suggestions[0]); return; }
            }
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              submit();
            }
            if (e.key === "Escape" && replyTo) onCancelReply();
          }}
          className="flex-1 resize-none bg-transparent outline-none text-[15px] text-slate-100 placeholder-slate-500 py-2 max-h-40"
        />
        {text.length > MAX - 200 && <span className="text-[11px] text-amber-300 pb-3">{MAX - text.length}</span>}
        <button
          onClick={() => setShowEmoji((s) => !s)}
          className={cn("p-2 rounded-xl transition-colors cursor-pointer", showEmoji ? "text-cyan-300 bg-cyan-400/10" : "text-slate-400 hover:text-white")}
          title="Emoji"
        >
          <Smile size={20} />
        </button>
        <button
          onClick={submit}
          disabled={!text.trim() || disabled}
          title="Send (Enter)"
          className="w-10 h-10 shrink-0 rounded-xl flex items-center justify-center bg-gradient-to-br from-cyan-400 to-violet-500 text-slate-950 shadow-[0_0_20px_-4px_rgba(34,211,238,0.8)] disabled:opacity-40 disabled:shadow-none transition-all hover:scale-105 active:scale-95 cursor-pointer"
        >
          <MdSend size={18} />
        </button>
      </div>
      <div className="text-[10px] text-slate-600 mt-1.5 px-1 hidden sm:block">Enter to send · Shift+Enter for a new line · @ to mention</div>
    </div>
  );
}
