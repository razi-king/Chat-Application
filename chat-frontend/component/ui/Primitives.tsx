"use client";
import React, { useRef } from "react";
import { cn } from "@/lib/utils";

// ---------- Logo: A Small CSS 3D Cube ----------
export const Logo = ({ size = 34, withText = true }: { size?: number; withText?: boolean }) => {
  const half = size / 2;
  const face = "absolute inset-0 rounded-[6px] border border-cyan-300/60";
  return (
    <div className="flex items-center gap-3">
      <div className="perspective" style={{ width: size, height: size }}>
        <div className="relative w-full h-full preserve-3d animate-[spin-cube_9s_linear_infinite]">
          <div className={cn(face, "bg-cyan-400/25")} style={{ transform: `translateZ(${half}px)` }} />
          <div className={cn(face, "bg-violet-400/25")} style={{ transform: `rotateY(180deg) translateZ(${half}px)` }} />
          <div className={cn(face, "bg-pink-400/20")} style={{ transform: `rotateY(90deg) translateZ(${half}px)` }} />
          <div className={cn(face, "bg-cyan-400/20")} style={{ transform: `rotateY(-90deg) translateZ(${half}px)` }} />
          <div className={cn(face, "bg-violet-400/20")} style={{ transform: `rotateX(90deg) translateZ(${half}px)` }} />
          <div className={cn(face, "bg-pink-400/20")} style={{ transform: `rotateX(-90deg) translateZ(${half}px)` }} />
        </div>
      </div>
      {withText && <span className="font-display text-xl font-bold tracking-tight text-white">Nexus</span>}
      <style>{`@keyframes spin-cube { from { transform: rotateX(-20deg) rotateY(0deg); } to { transform: rotateX(-20deg) rotateY(360deg); } }`}</style>
    </div>
  );
};

// ---------- TiltCard: Follows The Mouse In 3D ----------
export const TiltCard = ({ children, className = "", max = 12 }: { children: React.ReactNode; className?: string; max?: number }) => {
  const ref = useRef<HTMLDivElement>(null);
  const onMove = (e: React.MouseEvent) => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width - 0.5;
    const y = (e.clientY - r.top) / r.height - 0.5;
    el.style.transform = `perspective(900px) rotateX(${-y * max}deg) rotateY(${x * max}deg) translateZ(0)`;
    el.style.setProperty("--mx", `${(x + 0.5) * 100}%`);
    el.style.setProperty("--my", `${(y + 0.5) * 100}%`);
  };
  const reset = () => {
    if (ref.current) ref.current.style.transform = "perspective(900px) rotateX(0) rotateY(0)";
  };
  return (
    <div
      ref={ref}
      onMouseMove={onMove}
      onMouseLeave={reset}
      className={cn("group relative transition-transform duration-200 ease-out will-change-transform preserve-3d", className)}
    >
      <div
        className="pointer-events-none absolute inset-0 rounded-[inherit] opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        style={{ background: "radial-gradient(400px circle at var(--mx) var(--my), rgba(34,211,238,0.12), transparent 40%)" }}
      />
      {children}
    </div>
  );
};

export const Spinner = ({ size = 20, className = "" }: { size?: number; className?: string }) => (
  <div
    className={cn("rounded-full border-2 border-cyan-400/30 border-t-cyan-300 animate-spin", className)}
    style={{ width: size, height: size }}
  />
);

export const FullScreenLoader = ({ label = "Booting Nexus..." }: { label?: string }) => (
  <div className="fixed inset-0 bg-aurora flex flex-col items-center justify-center gap-6">
    <Logo size={56} withText={false} />
    <div className="font-mono text-xs uppercase tracking-[0.3em] text-cyan-300/80 animate-glow">{label}</div>
  </div>
);

export const EmptyState = ({
  icon,
  title,
  text,
  action,
}: {
  icon?: React.ReactNode;
  title: string;
  text?: string;
  action?: React.ReactNode;
}) => (
  <div className="flex flex-col items-center justify-center text-center gap-3 p-10">
    {icon && (
      <div className="w-16 h-16 rounded-2xl glass flex items-center justify-center text-cyan-300 glow-cyan animate-float">
        {icon}
      </div>
    )}
    <h3 className="font-display text-lg font-semibold text-white">{title}</h3>
    {text && <p className="text-sm text-muted max-w-sm">{text}</p>}
    {action}
  </div>
);

export const Badge = ({ count, className = "" }: { count: number; className?: string }) =>
  count > 0 ? (
    <span
      className={cn(
        "min-w-5 h-5 px-1.5 rounded-full bg-gradient-to-r from-cyan-400 to-violet-400 text-[11px] font-bold text-slate-950 flex items-center justify-center shadow-[0_0_12px_rgba(34,211,238,0.6)] animate-pop",
        className
      )}
    >
      {count > 99 ? "99+" : count}
    </span>
  ) : null;

export const RoleBadge = ({ role }: { role?: string }) =>
  role && role !== "MEMBER" ? (
    <span
      className={cn(
        "text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded",
        role === "OWNER" ? "bg-amber-400/15 text-amber-300" : "bg-violet-400/15 text-violet-300"
      )}
    >
      {role.toLowerCase()}
    </span>
  ) : null;
