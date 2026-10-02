"use client";
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import Link from 'next/link';
import toast from 'react-hot-toast';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowDown, ArrowLeft, Hash, Info, Lock, Pin, Search, Users } from 'lucide-react';
import { FormButton } from '../enums/ButtonStyles';
import Avatar from '../ui/Avatar';
import { EmptyState, Spinner } from '../ui/Primitives';
import MessageItem, { ReadState } from './MessageItem';
import Composer from './Composer';
import { MemberListPanel, PinnedPanel, RoomInfoPanel, SearchPanel } from './ChatPanels';
import { RoomService } from '@/services/RoomService';
import { MessageService } from '@/services/MessageService';
import { useAuth } from '@/context/AuthContext';
import { useSocket } from '@/context/SocketContext';
import { useAppState } from '@/context/AppStateContext';
import { useServerContext } from '@/context/ServerContext';
import { handleError, getErrorMessage } from '@/lib/errorHandler';
import { cn, formatDay, lastSeenText, newClientId, roomTitle } from '@/lib/utils';
import type { Message, ReadEvent, Room, TypingEvent, User } from '@/types';

type Panel = "info" | "members" | "pinned" | "search" | null;
const GROUP_WINDOW_MS = 5 * 60 * 1000;
const PENDING_TIMEOUT_MS = 8000;

interface Props {
  roomId: string;
  backHref: string;
}

// The Chat Window Used For DMs, Groups (Bubble Layout) And Server Channels (Discord Layout)
const ChatPage = ({ roomId, backHref }: Props) => {
  const { user } = useAuth();
  const me = user as User;
  const serverCtx = useServerContext();
  const { subscribe, publish } = useSocket();
  const { setActiveRoom, clearUnread, isOnline, refreshConversations } = useAppState();

  const [room, setRoom] = useState<Room | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [typing, setTyping] = useState<Record<string, { name: string; until: number }>>({});
  const [reads, setReads] = useState<Record<string, string>>({});
  const [replyTo, setReplyTo] = useState<Message | null>(null);
  const [panel, setPanel] = useState<Panel>(null);
  const [highlight, setHighlight] = useState<string | null>(null);
  const [pinVersion, setPinVersion] = useState(0);
  const [showJump, setShowJump] = useState(false);

  const scrollRef = useRef<HTMLDivElement>(null);
  const nearBottom = useRef(true);
  const lastMessageId = useRef<string | null>(null);
  const prependHeight = useRef<number | null>(null);

  const isChannel = room?.type === "CHANNEL";
  const layout = isChannel ? "flat" : "bubble";

  const applyRoom = useCallback((r: Room) => {
    setRoom(r);
    const map: Record<string, string> = {};
    r.members?.forEach((m) => { if (m.lastReadAt) map[m.user.id] = m.lastReadAt; });
    setReads(map);
  }, []);

  const reloadRoom = useCallback(async () => {
    try {
      applyRoom(await RoomService.getRoom(roomId));
      refreshConversations();
    } catch (e) {
      handleError(e);
    }
  }, [roomId, applyRoom, refreshConversations]);

  // ---------- Initial Load ----------
  useEffect(() => {
    let cancelled = false;
    setActiveRoom(roomId);
    Promise.all([RoomService.getRoom(roomId), RoomService.getMessages(roomId)])
      .then(([r, page]) => {
        if (cancelled) return;
        applyRoom(r);
        setMessages(page.items);
        setHasMore(page.hasNext);
        if (r.type === "CHANNEL" && window.innerWidth >= 1280) setPanel("members");
      })
      .catch((e) => !cancelled && setError(getErrorMessage(handleError(e, { silent: true }))));
    RoomService.markRead(roomId).then(() => clearUnread(roomId)).catch(() => {});
    return () => {
      cancelled = true;
      setActiveRoom(null);
    };
  }, [roomId, applyRoom, setActiveRoom, clearUnread]);

  // ---------- Live Events For This Room ----------
  useEffect(() => {
    return subscribe(`/topic/rooms/${roomId}`, (event) => {
      switch (event.type) {
        case "MESSAGE_CREATED": {
          const m = event.payload as Message;
          setMessages((prev) => {
            if (prev.some((x) => x.id === m.id)) return prev;
            // Our Own Optimistic Copy (Same clientMessageId) -> Swap It For The Saved One
            if (m.clientMessageId) {
              const idx = prev.findIndex((x) => x.id === m.clientMessageId);
              if (idx >= 0) {
                const copy = [...prev];
                copy[idx] = m;
                return copy;
              }
            }
            return [...prev, m];
          });
          // The Server Pushes UNREAD Only To OTHER Members, So The Sender Refreshes Its Own Chat List Preview
          if (m.sender?.id === me.id) refreshConversations();
          if (m.sender) {
            setTyping((t) => {
              const { [m.sender!.id]: _gone, ...rest } = t;
              return rest;
            });
          }
          break;
        }
        case "MESSAGE_UPDATED":
        case "MESSAGE_DELETED": {
          const m = event.payload as Message;
          setMessages((prev) => prev.map((x) => (x.id === m.id ? m : x)));
          setPinVersion((v) => v + 1);
          break;
        }
        case "TYPING": {
          const p = event.payload as TypingEvent;
          if (p.userId === me.id) return;
          setTyping((t) => {
            if (!p.typing) {
              const { [p.userId]: _gone, ...rest } = t;
              return rest;
            }
            return { ...t, [p.userId]: { name: p.displayName, until: Date.now() + 5000 } };
          });
          break;
        }
        case "READ": {
          const p = event.payload as ReadEvent;
          setReads((r) => ({ ...r, [p.userId]: p.lastReadAt }));
          break;
        }
      }
    });
  }, [roomId, subscribe, me.id, refreshConversations]);

  // Typing Bubbles Expire If The "Stopped" Event Never Arrives
  useEffect(() => {
    const t = setInterval(() => {
      setTyping((cur) => {
        const now = Date.now();
        const next = Object.fromEntries(Object.entries(cur).filter(([, v]) => v.until > now));
        return Object.keys(next).length === Object.keys(cur).length ? cur : next;
      });
    }, 1000);
    return () => clearInterval(t);
  }, []);

  // ---------- Scrolling ----------
  const scrollToBottom = (behavior: ScrollBehavior = "smooth") => {
    const el = scrollRef.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior });
  };

  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    // Older Page Prepended -> Keep The Same Message Under The User's Eyes
    if (prependHeight.current !== null) {
      el.scrollTop += el.scrollHeight - prependHeight.current;
      prependHeight.current = null;
      return;
    }
    const last = messages[messages.length - 1];
    if (!last || last.id === lastMessageId.current) return;
    const first = lastMessageId.current === null;
    lastMessageId.current = last.id;
    if (first) {
      el.scrollTop = el.scrollHeight;
    } else if (nearBottom.current || last.sender?.id === me.id) {
      scrollToBottom();
    } else {
      setShowJump(true);
    }
  }, [messages, me.id]);

  const loadOlder = useCallback(async () => {
    if (loadingOlder || !hasMore || messages.length === 0) return;
    setLoadingOlder(true);
    try {
      const page = await RoomService.getMessages(roomId, messages[0].createdAt);
      prependHeight.current = scrollRef.current?.scrollHeight ?? null;
      setMessages((prev) => {
        const ids = new Set(prev.map((m) => m.id));
        return [...page.items.filter((m) => !ids.has(m.id)), ...prev];
      });
      setHasMore(page.hasNext);
    } catch (e) {
      handleError(e);
    } finally {
      setLoadingOlder(false);
    }
  }, [loadingOlder, hasMore, messages, roomId]);

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    nearBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 140;
    if (nearBottom.current) setShowJump(false);
    if (el.scrollTop < 80) loadOlder();
  };

  // ---------- Sending (WebSocket First, REST Fallback) ----------
  const markFailed = (tempId: string) =>
    setMessages((prev) => prev.map((x) => (x.id === tempId && x.pending ? { ...x, pending: false, failed: true } : x)));

  const sendViaRest = async (tempId: string, content: string, replyId?: string) => {
    try {
      // tempId Doubles As The Idempotency Key, So If The WebSocket Send Actually Succeeded
      // The Server Returns That Same Message Instead Of Storing A Second Copy
      const saved = await RoomService.sendMessage(roomId, content, replyId, tempId);
      setMessages((prev) =>
        prev.some((x) => x.id === saved.id) ? prev.filter((x) => x.id !== tempId) : prev.map((x) => (x.id === tempId ? saved : x))
      );
    } catch (e) {
      handleError(e);
      markFailed(tempId);
    }
  };

  const send = (content: string, reply: Message | null = replyTo) => {
    const tempId = newClientId();
    const temp: Message = {
      id: tempId, roomId, sender: me, content, type: "TEXT", reactions: [], mentions: [], edited: false, deleted: false,
      pinned: false, createdAt: new Date().toISOString(), pending: true,
      replyTo: reply ? { id: reply.id, senderId: reply.sender?.id ?? "", senderName: reply.sender?.displayName ?? "", content: reply.content, deleted: false } : undefined,
    };
    setMessages((prev) => [...prev, temp]);
    setReplyTo(null);
    try {
      publish(`/app/rooms/${roomId}/send`, { content, replyToId: reply?.id, clientMessageId: tempId });
      setTimeout(() => markFailed(tempId), PENDING_TIMEOUT_MS);
    } catch {
      sendViaRest(tempId, content, reply?.id);
    }
  };

  const retry = (m: Message) => {
    setMessages((prev) => prev.map((x) => (x.id === m.id ? { ...x, pending: true, failed: false } : x)));
    sendViaRest(m.id, m.content, m.replyTo?.id);
  };

  const onTyping = (isTyping: boolean) => {
    try {
      publish(`/app/rooms/${roomId}/typing`, { typing: isTyping });
    } catch {
      // Offline: Typing Is Best Effort
    }
  };

  const replace = (m: Message) => setMessages((prev) => prev.map((x) => (x.id === m.id ? m : x)));
  const guarded = async (fn: () => Promise<Message>) => {
    try {
      replace(await fn());
    } catch (e) {
      handleError(e);
    }
  };

  const jumpTo = (id: string) => {
    const el = document.getElementById(`msg-${id}`);
    if (!el) {
      toast("That message is further up. Scroll to load older messages.", { icon: "🕓" });
      return;
    }
    el.scrollIntoView({ behavior: "smooth", block: "center" });
    setHighlight(id);
    setTimeout(() => setHighlight(null), 1800);
  };

  // ---------- Derived ----------
  const others = useMemo(() => room?.members?.filter((m) => m.user.id !== me.id) ?? [], [room, me.id]);
  const mentionables = useMemo<User[]>(() => {
    const list = serverCtx && isChannel ? serverCtx.detail.members.map((m) => m.user) : room?.members?.map((m) => m.user) ?? [];
    return list.filter((u) => u.id !== me.id);
  }, [serverCtx, isChannel, room, me.id]);

  const readState = (m: Message): ReadState | undefined => {
    if (m.sender?.id !== me.id || isChannel) return undefined;
    if (m.failed) return "failed";
    if (m.pending) return "pending";
    if (others.length === 0) return "sent";
    const created = Date.parse(m.createdAt);
    const readCount = others.filter((o) => reads[o.user.id] && Date.parse(reads[o.user.id]) >= created).length;
    return readCount === others.length ? "read" : readCount > 0 ? "partial" : "sent";
  };

  const canModerate = isChannel
    ? serverCtx?.detail.server.myRole === "OWNER" || serverCtx?.detail.server.myRole === "ADMIN"
    : room?.myRole === "OWNER" || room?.myRole === "ADMIN";
  const typingNames = Object.values(typing).map((t) => t.name.split(" ")[0]);
  const typingText = typingNames.length === 0 ? null : typingNames.length === 1 ? `${typingNames[0]} is typing` : `${typingNames.slice(0, 2).join(", ")} are typing`;

  if (error) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <EmptyState icon={<Lock size={26} />} title="Can't open this chat" text={error}
          action={<Link href={backHref} className={`${FormButton.SECONDARY} mt-2`}>Go back</Link>} />
      </div>
    );
  }

  const otherUser = room?.otherUser;
  const subtitle = typingText
    ? null
    : room?.type === "DIRECT"
      ? lastSeenText(otherUser, isOnline(otherUser))
      : room?.type === "GROUP"
        ? room.members?.map((m) => (m.user.id === me.id ? "You" : m.user.displayName.split(" ")[0])).join(", ")
        : room?.description;

  return (
    <div className="flex-1 flex min-w-0 relative">
      <div className="flex-1 flex flex-col min-w-0 bg-grid">
        {/* ---------- Header ---------- */}
        <header className="h-16 shrink-0 flex items-center gap-3 px-4 border-b border-line bg-surface/60 backdrop-blur-xl">
          <Link href={backHref} className="md:hidden text-slate-400 hover:text-white"><ArrowLeft size={20} /></Link>
          {!room ? (
            <div className="h-8 w-48 rounded-lg skeleton" />
          ) : (
            <button onClick={() => setPanel(isChannel ? "members" : "info")} className="flex items-center gap-3 min-w-0 text-left cursor-pointer">
              {isChannel ? (
                <Hash size={22} className="text-slate-400 shrink-0" />
              ) : room.type === "DIRECT" ? (
                <Avatar name={otherUser?.displayName} color={otherUser?.avatarColor} showStatus online={isOnline(otherUser)} />
              ) : (
                <Avatar name={room.name} color={room.iconColor} square />
              )}
              <div className="min-w-0">
                <h1 className="font-semibold text-white truncate">{roomTitle(room)}</h1>
                {typingText ? (
                  <p className="text-xs text-cyan-300 animate-glow">{typingText}...</p>
                ) : (
                  subtitle && <p className={cn("text-xs truncate", subtitle === "online" ? "text-emerald-300" : "text-slate-500")}>{subtitle}</p>
                )}
              </div>
            </button>
          )}
          <div className="ml-auto flex items-center gap-1">
            <HeaderButton title="Search" active={panel === "search"} onClick={() => setPanel(panel === "search" ? null : "search")}><Search size={18} /></HeaderButton>
            <HeaderButton title="Pinned" active={panel === "pinned"} onClick={() => setPanel(panel === "pinned" ? null : "pinned")}><Pin size={18} /></HeaderButton>
            {isChannel ? (
              <HeaderButton title="Members" active={panel === "members"} onClick={() => setPanel(panel === "members" ? null : "members")}><Users size={18} /></HeaderButton>
            ) : (
              <HeaderButton title="Info" active={panel === "info"} onClick={() => setPanel(panel === "info" ? null : "info")}><Info size={18} /></HeaderButton>
            )}
          </div>
        </header>

        {/* ---------- Messages ---------- */}
        <div ref={scrollRef} onScroll={onScroll} className="flex-1 overflow-y-auto pb-4 relative">
          {!room && (
            <div className="p-6 space-y-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className={cn("h-12 rounded-2xl skeleton", i % 2 ? "ml-auto w-1/2" : "w-2/3")} />
              ))}
            </div>
          )}
          {loadingOlder && <div className="flex justify-center py-3"><Spinner /></div>}
          {room && !hasMore && <RoomIntro room={room} />}
          {messages.map((m, i) => {
            const prev = messages[i - 1];
            const newDay = !prev || new Date(prev.createdAt).toDateString() !== new Date(m.createdAt).toDateString();
            const grouped =
              !!prev && !newDay && prev.type !== "SYSTEM" && m.type !== "SYSTEM" && !m.replyTo &&
              prev.sender?.id === m.sender?.id && Date.parse(m.createdAt) - Date.parse(prev.createdAt) < GROUP_WINDOW_MS;
            return (
              <React.Fragment key={m.id}>
                {newDay && (
                  <div className="flex items-center gap-3 px-6 my-5">
                    <div className="flex-1 h-px bg-gradient-to-r from-transparent via-line to-transparent" />
                    <span className="text-[11px] font-semibold uppercase tracking-widest text-slate-500 glass-soft rounded-full px-3 py-1">{formatDay(m.createdAt)}</span>
                    <div className="flex-1 h-px bg-gradient-to-r from-transparent via-line to-transparent" />
                  </div>
                )}
                <MessageItem
                  message={m}
                  layout={layout}
                  mine={m.sender?.id === me.id}
                  grouped={grouped}
                  showSenderName={room?.type !== "DIRECT"}
                  meId={me.id}
                  readState={readState(m)}
                  highlighted={highlight === m.id}
                  canModerate={!!canModerate}
                  canPin={!isChannel || !!canModerate}
                  onReply={setReplyTo}
                  onReact={(msg, emoji) => guarded(() => MessageService.react(msg.id, emoji))}
                  onEdit={(msg, content) => guarded(() => MessageService.edit(msg.id, content))}
                  onDelete={(msg) => confirm("Delete this message?") && guarded(() => MessageService.remove(msg.id))}
                  onPin={(msg) => guarded(() => MessageService.togglePin(msg.id)).then(() => setPinVersion((v) => v + 1))}
                  onRetry={retry}
                  onJumpTo={jumpTo}
                />
              </React.Fragment>
            );
          })}
          <AnimatePresence>
            {typingText && (
              <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="px-6 mt-3 flex items-center gap-2 text-xs text-slate-400">
                <span className="flex gap-1">
                  {[0, 1, 2].map((d) => (
                    <span key={d} className="w-1.5 h-1.5 rounded-full bg-cyan-300 animate-bounce" style={{ animationDelay: `${d * 0.15}s` }} />
                  ))}
                </span>
                {typingText}
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {showJump && (
          <button onClick={() => { scrollToBottom(); setShowJump(false); }}
            className="absolute bottom-28 left-1/2 -translate-x-1/2 z-10 glass glow-cyan rounded-full px-4 py-2 text-sm text-white flex items-center gap-2 cursor-pointer animate-pop">
            <ArrowDown size={15} /> New messages
          </button>
        )}

        <Composer
          placeholder={room ? (isChannel ? `Message #${room.name}` : `Message ${roomTitle(room)}`) : "Loading..."}
          replyTo={replyTo}
          onCancelReply={() => setReplyTo(null)}
          onSend={(c) => send(c)}
          onTyping={onTyping}
          mentionables={mentionables}
          disabled={!room}
        />
      </div>

      {/* ---------- Right Panel ---------- */}
      {room && panel === "members" && serverCtx && (
        <MemberListPanel detail={serverCtx.detail} reload={serverCtx.reload} onClose={() => setPanel(null)} />
      )}
      {room && panel === "info" && <RoomInfoPanel room={room} onClose={() => setPanel(null)} onChanged={reloadRoom} />}
      {room && panel === "pinned" && <PinnedPanel roomId={roomId} version={pinVersion} onClose={() => setPanel(null)} onJump={jumpTo} />}
      {room && panel === "search" && <SearchPanel roomId={roomId} onClose={() => setPanel(null)} onJump={jumpTo} />}
    </div>
  )
}

function HeaderButton({ children, title, active, onClick }: { children: React.ReactNode; title: string; active: boolean; onClick: () => void }) {
  return (
    <button title={title} onClick={onClick}
      className={cn("p-2 rounded-xl transition-colors cursor-pointer", active ? "text-cyan-300 bg-cyan-400/10" : "text-slate-400 hover:text-white hover:bg-white/5")}>
      {children}
    </button>
  );
}

// "This Is The Beginning Of..." Header Shown Above The Oldest Message
function RoomIntro({ room }: { room: Room }) {
  if (room.type === "CHANNEL") {
    return (
      <div className="px-6 pt-10 pb-4">
        <div className="w-16 h-16 rounded-2xl glass flex items-center justify-center text-cyan-300 glow-cyan"><Hash size={32} /></div>
        <h2 className="font-display text-3xl font-bold text-white mt-4">Welcome to #{room.name}</h2>
        <p className="text-slate-400 mt-1">This is the start of the #{room.name} channel. {room.description}</p>
      </div>
    );
  }
  const isDm = room.type === "DIRECT";
  return (
    <div className="flex flex-col items-center text-center px-6 pt-10 pb-6">
      {isDm ? (
        <Avatar name={room.otherUser?.displayName} color={room.otherUser?.avatarColor} size="xl" />
      ) : (
        <Avatar name={room.name} color={room.iconColor} size="xl" square />
      )}
      <h2 className="font-display text-2xl font-bold text-white mt-4">{roomTitle(room)}</h2>
      <p className="text-sm text-slate-400 mt-1 max-w-sm">
        {isDm ? `This is the beginning of your conversation with ${room.otherUser?.displayName}.` : `Group created. Say hello to everyone!`}
      </p>
      <div className="mt-4 text-[11px] text-amber-200/80 bg-amber-400/10 border border-amber-300/20 rounded-lg px-3 py-1.5 flex items-center gap-1.5">
        <Lock size={11} /> Only members of this chat can read these messages
      </div>
    </div>
  );
}

export default ChatPage
