"use client";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { motion } from "framer-motion";
import { Compass, Users } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { EmptyState, Spinner, TiltCard } from "@/component/ui/Primitives";
import { FormButton } from "@/component/enums/ButtonStyles";
import { ServerService } from "@/services/ServerService";
import { useAppState } from "@/context/AppStateContext";
import { handleError } from "@/lib/errorHandler";
import type { Server } from "@/types";

export default function DiscoverPage() {
  const router = useRouter();
  const { refreshServers } = useAppState();
  const [servers, setServers] = useState<Server[] | null>(null);

  useEffect(() => {
    ServerService.discover().then(setServers).catch((e) => { handleError(e); setServers([]); });
  }, []);

  const join = async (s: Server) => {
    if (s.myRole) {
      router.push(`/app/servers/${s.id}`);
      return;
    }
    try {
      await ServerService.join(s.id);
      toast.success(`Welcome to ${s.name}!`);
      await refreshServers();
      router.push(`/app/servers/${s.id}`);
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <div className="p-6 lg:p-10 max-w-6xl">
      <div className="relative overflow-hidden rounded-3xl glass neon-border p-8 lg:p-12">
        <div className="absolute -right-10 -top-10 w-64 h-64 rounded-full bg-violet-500/20 blur-3xl" />
        <div className="absolute right-40 bottom-0 w-48 h-48 rounded-full bg-cyan-400/20 blur-3xl" />
        <Compass className="text-cyan-300" size={32} />
        <h1 className="font-display text-4xl font-bold text-white mt-4">Find your community</h1>
        <p className="text-slate-400 mt-2 max-w-lg">Public servers anyone can join. Make yours public from the create server dialog.</p>
      </div>
      {servers === null && <div className="flex justify-center py-16"><Spinner size={30} /></div>}
      {servers?.length === 0 && <EmptyState icon={<Compass size={26} />} title="No public servers yet" text="Create a server and set it to Public to list it here." />}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 mt-8">
        {servers?.map((s, i) => (
          <motion.div key={s.id} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
            <TiltCard className="glass rounded-2xl overflow-hidden h-full flex flex-col">
              <div className="h-24 relative" style={{ background: `linear-gradient(135deg, ${s.iconColor}55, transparent), radial-gradient(circle at 80% 20%, ${s.iconColor}44, transparent 60%)` }}>
                <div className="absolute -bottom-6 left-5"><Avatar name={s.name} color={s.iconColor} size="lg" square /></div>
              </div>
              <div className="p-5 pt-9 flex-1 flex flex-col">
                <h3 className="font-display text-lg font-semibold text-white">{s.name}</h3>
                <p className="text-sm text-slate-400 mt-1 flex-1">{s.description || "No description"}</p>
                <div className="flex items-center justify-between mt-5">
                  <span className="text-xs text-slate-500 flex items-center gap-1"><Users size={13} /> {s.memberCount} members</span>
                  <button onClick={() => join(s)} className={`${s.myRole ? FormButton.SECONDARY : FormButton.PRIMARY} !w-auto !py-1.5 !px-4 text-sm cursor-pointer`}>
                    {s.myRole ? "Open" : "Join"}
                  </button>
                </div>
              </div>
            </TiltCard>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
