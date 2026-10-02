"use client";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  MessageCircle, Server, Zap, ShieldCheck, Radio, Bell, Users, Hash, CheckCheck, Smile, Pin, Search,
} from "lucide-react";
import Scene3D from "@/component/three/Scene3D";
import { TiltCard } from "@/component/ui/Primitives";
import { FormButton } from "@/component/enums/ButtonStyles";
import { useAuth } from "@/context/AuthContext";

const features = [
  { icon: MessageCircle, title: "WhatsApp-style chats", text: "1-to-1 DMs and groups with read ticks, replies, reactions and last seen.", color: "#34d399" },
  { icon: Server, title: "Discord-style servers", text: "Communities with channels, roles, invite links and a full audit log.", color: "#a78bfa" },
  { icon: Zap, title: "Real-time over STOMP", text: "WebSockets push messages, typing indicators and presence instantly.", color: "#22d3ee" },
  { icon: Radio, title: "Redis-powered", text: "Online status, unread counters, rate limiting and logout blacklist live in Redis.", color: "#f472b6" },
  { icon: ShieldCheck, title: "JWT + custom errors", text: "Every API answers with one Response DTO and a typed error code, front to back.", color: "#fbbf24" },
  { icon: Bell, title: "Live notifications", text: "Mentions, friend requests and server joins arrive the moment they happen.", color: "#60a5fa" },
];

const stack = ["Spring Boot 3", "Java 17", "MongoDB", "Redis", "STOMP WebSocket", "JWT", "Next.js 16", "React 19", "Three.js", "Tailwind 4"];

export default function Home() {
  const { user } = useAuth();
  return (
    <div className="bg-aurora min-h-screen overflow-x-hidden">
      {/* ---------- Hero ---------- */}
      <section className="relative min-h-screen flex items-center">
        <div className="absolute inset-0 bg-grid [mask-image:radial-gradient(ellipse_at_center,black_30%,transparent_75%)]" />
        <div className="relative mx-auto max-w-6xl w-full px-4 pt-28 pb-16 grid lg:grid-cols-2 gap-10 items-center">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="relative z-10"
          >
            <div className="inline-flex items-center gap-2 glass-soft rounded-full px-3 py-1 text-xs text-cyan-200 mb-6">
              <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_8px_#34d399] animate-pulse" />
              Realtime network online
            </div>
            <h1 className="font-display text-5xl sm:text-6xl lg:text-7xl font-bold leading-[1.02] tracking-tight text-white">
              Chat at the
              <br />
              <span className="text-gradient animate-gradient">speed of light.</span>
            </h1>
            <p className="mt-6 text-lg text-slate-400 max-w-xl leading-relaxed">
              Nexus fuses the intimacy of WhatsApp with the communities of Discord. Private chats, groups, servers and
              channels, all live, all in one futuristic hub.
            </p>
            <div className="mt-10 flex flex-wrap gap-4">
              <Link href={user ? "/app" : "/register"} className={`${FormButton.PRIMARY} !w-auto px-7 py-3 text-base`}>
                {user ? "Open Nexus" : "Create free account"}
              </Link>
              <Link href={user ? "/join-chat" : "/login"} className={`${FormButton.SECONDARY} px-7 py-3 text-base`}>
                {user ? "Join with invite" : "I have an account"}
              </Link>
            </div>
            <div className="mt-10 flex items-center gap-6 text-sm text-slate-500">
              <span className="flex items-center gap-2"><CheckCheck size={16} className="text-sky-400" /> Read receipts</span>
              <span className="flex items-center gap-2"><Hash size={16} className="text-violet-400" /> Channels</span>
              <span className="flex items-center gap-2"><Users size={16} className="text-emerald-400" /> Presence</span>
            </div>
          </motion.div>

          <div className="relative h-[420px] sm:h-[520px] lg:h-[600px]">
            <Scene3D variant="hero" />
            {/* Floating Chat Cards Over The 3D Scene */}
            <motion.div
              initial={{ opacity: 0, x: 40 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.6 }}
              className="absolute right-0 top-10 glass rounded-2xl px-4 py-3 w-60 animate-float"
            >
              <div className="text-xs text-cyan-300 font-semibold"># general · Nexus HQ</div>
              <div className="text-sm text-slate-200 mt-1">Redis presence is live 🚀</div>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: -40 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.9 }}
              className="absolute left-0 bottom-16 glass rounded-2xl px-4 py-3 w-56 [animation-delay:1.5s] animate-float"
            >
              <div className="text-xs text-emerald-300 font-semibold">Aisha · DM</div>
              <div className="text-sm text-slate-200 mt-1 flex items-end justify-between gap-2">
                Send me the link! <CheckCheck size={14} className="text-sky-400 shrink-0" />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ---------- Features ---------- */}
      <section id="features" className="relative mx-auto max-w-6xl px-4 py-24">
        <div className="text-center mb-16">
          <p className="font-mono text-xs uppercase tracking-[0.3em] text-cyan-300">Feature matrix</p>
          <h2 className="font-display text-4xl font-bold text-white mt-3">Two worlds. One network.</h2>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08 }}
            >
              <TiltCard className="glass rounded-2xl p-6 h-full">
                <div
                  className="w-12 h-12 rounded-xl flex items-center justify-center mb-5"
                  style={{ background: `${f.color}1f`, boxShadow: `0 0 24px -6px ${f.color}`, transform: "translateZ(30px)" }}
                >
                  <f.icon size={22} color={f.color} />
                </div>
                <h3 className="font-display text-lg font-semibold text-white" style={{ transform: "translateZ(20px)" }}>{f.title}</h3>
                <p className="text-sm text-slate-400 mt-2 leading-relaxed">{f.text}</p>
              </TiltCard>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ---------- Chat Preview ---------- */}
      <section className="mx-auto max-w-6xl px-4 pb-24">
        <TiltCard max={6} className="glass neon-border rounded-3xl p-2">
          <div className="rounded-[20px] bg-surface/80 grid grid-cols-[64px_1fr] md:grid-cols-[64px_220px_1fr] min-h-[340px] overflow-hidden">
            <div className="border-r border-line flex flex-col items-center gap-3 py-4">
              {["#22d3ee", "#a78bfa", "#34d399", "#f472b6"].map((c) => (
                <div key={c} className="w-10 h-10 rounded-2xl" style={{ background: `${c}33`, boxShadow: `inset 0 0 0 1px ${c}88` }} />
              ))}
            </div>
            <div className="hidden md:block border-r border-line p-4 space-y-2">
              <div className="text-xs uppercase tracking-widest text-slate-500 mb-3">Channels</div>
              {["general", "announcements", "off-topic"].map((c, i) => (
                <div key={c} className={`px-3 py-2 rounded-lg text-sm ${i === 0 ? "bg-cyan-400/10 text-white" : "text-slate-400"}`}># {c}</div>
              ))}
            </div>
            <div className="p-6 flex flex-col gap-4 justify-end">
              <Bubble who="Kabir" color="#34d399" text="Redis presence + unread counters are live. Try two tabs." />
              <Bubble who="Aisha" color="#f472b6" text="The 3D landing page looks insane 🔥" reactions="🔥 3   ❤️ 2" />
              <div className="self-end max-w-sm rounded-2xl rounded-br-md bg-gradient-to-br from-cyan-500/80 to-violet-500/80 px-4 py-2.5 text-sm text-white">
                Shipping reactions, replies and pins tonight 🚀
                <div className="flex justify-end items-center gap-1 text-[10px] text-white/70 mt-1">21:04 <CheckCheck size={12} className="text-sky-200" /></div>
              </div>
              <div className="glass-soft rounded-xl px-4 py-3 flex items-center gap-3 text-slate-500 text-sm">
                <Smile size={18} /> Message #general <Pin size={16} className="ml-auto" /> <Search size={16} />
              </div>
            </div>
          </div>
        </TiltCard>
      </section>

      {/* ---------- Stack ---------- */}
      <section className="mx-auto max-w-6xl px-4 pb-28 text-center">
        <p className="font-mono text-xs uppercase tracking-[0.3em] text-violet-300 mb-6">Built with</p>
        <div className="flex flex-wrap justify-center gap-3">
          {stack.map((s) => (
            <span key={s} className="glass-soft rounded-full px-4 py-2 text-sm text-slate-300 hover:text-white hover:border-cyan-400/40 transition-colors">
              {s}
            </span>
          ))}
        </div>
        <p className="mt-16 text-sm text-slate-600">© {new Date().getFullYear()} Nexus · Crafted by Razi</p>
      </section>
    </div>
  );
}

function Bubble({ who, color, text, reactions }: { who: string; color: string; text: string; reactions?: string }) {
  return (
    <div className="flex gap-3 max-w-md">
      <div className="w-9 h-9 rounded-full shrink-0" style={{ background: `radial-gradient(circle at 30% 30%, #fff9, ${color})` }} />
      <div>
        <div className="text-xs font-semibold" style={{ color }}>{who}</div>
        <div className="glass-soft rounded-2xl rounded-tl-md px-4 py-2.5 text-sm text-slate-200 mt-1">{text}</div>
        {reactions && <div className="text-xs mt-1 text-slate-400">{reactions}</div>}
      </div>
    </div>
  );
}
