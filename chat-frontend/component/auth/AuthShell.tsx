"use client";
import React from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import Scene3D from "@/component/three/Scene3D";
import { Logo } from "@/component/ui/Primitives";

// Split Screen: 3D Network On The Left, Glass Form On The Right
export default function AuthShell({ children, tagline }: { children: React.ReactNode; tagline: string }) {
  return (
    <div className="min-h-screen bg-aurora grid lg:grid-cols-2">
      <div className="relative hidden lg:flex flex-col justify-between p-10 overflow-hidden border-r border-line">
        <div className="absolute inset-0 bg-grid opacity-60 [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
        <Scene3D variant="auth" />
        <Link href="/" className="relative z-10"><Logo /></Link>
        <div className="relative z-10 max-w-md">
          <h2 className="font-display text-4xl font-bold text-white leading-tight">{tagline}</h2>
          <p className="mt-4 text-slate-400">DMs, groups, servers and channels. Synced in real time across every device.</p>
        </div>
      </div>
      <div className="relative flex items-center justify-center px-4 py-12">
        <div className="lg:hidden absolute top-6 left-6"><Link href="/"><Logo size={26} /></Link></div>
        <motion.div
          initial={{ opacity: 0, y: 24, rotateX: -8 }}
          animate={{ opacity: 1, y: 0, rotateX: 0 }}
          transition={{ type: "spring", stiffness: 120, damping: 18 }}
          style={{ transformPerspective: 1000 }}
          className="w-full max-w-md glass neon-border rounded-3xl p-8 shadow-2xl"
        >
          {children}
        </motion.div>
      </div>
    </div>
  );
}
