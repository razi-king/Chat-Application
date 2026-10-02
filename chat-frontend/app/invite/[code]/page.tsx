"use client";
import React, { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import toast from "react-hot-toast";
import { Users } from "lucide-react";
import Scene3D from "@/component/three/Scene3D";
import Avatar from "@/component/ui/Avatar";
import { Logo, Spinner } from "@/component/ui/Primitives";
import { FormButton } from "@/component/enums/ButtonStyles";
import { InviteService } from "@/services/ServerService";
import { useAuth } from "@/context/AuthContext";
import { getErrorMessage, handleError } from "@/lib/errorHandler";
import type { Invite } from "@/types";

export default function InvitePage() {
  const { code } = useParams<{ code: string }>();
  const router = useRouter();
  const { user, loading } = useAuth();
  const [invite, setInvite] = useState<Invite | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [joining, setJoining] = useState(false);

  useEffect(() => {
    InviteService.preview(code).then(setInvite).catch((e) => setError(getErrorMessage(e)));
  }, [code]);

  const join = async () => {
    if (!user) {
      router.push(`/login?next=${encodeURIComponent(`/invite/${code}`)}`);
      return;
    }
    setJoining(true);
    try {
      const server = await InviteService.accept(code);
      toast.success(`Welcome to ${server.name}!`);
      router.push(`/app/servers/${server.id}`);
    } catch (e) {
      handleError(e);
      setJoining(false);
    }
  };

  return (
    <div className="relative min-h-screen bg-aurora flex items-center justify-center p-4 overflow-hidden">
      <Scene3D variant="auth" className="opacity-60" />
      <div className="absolute top-6 left-6"><Link href="/"><Logo size={26} /></Link></div>
      <div className="relative w-full max-w-sm glass neon-border rounded-3xl p-8 text-center">
        {!invite && !error && <div className="flex justify-center py-10"><Spinner size={32} /></div>}
        {error && (
          <>
            <h1 className="font-display text-2xl font-bold text-white">Invite unavailable</h1>
            <p className="text-slate-400 mt-2">{error}</p>
            <Link href="/" className={`${FormButton.SECONDARY} inline-block mt-6`}>Go home</Link>
          </>
        )}
        {invite && (
          <>
            <p className="text-xs uppercase tracking-[0.25em] text-cyan-300">You have been invited to</p>
            <div className="flex justify-center my-6">
              <Avatar name={invite.serverName} color={invite.serverIconColor} size="xl" square />
            </div>
            <h1 className="font-display text-3xl font-bold text-white">{invite.serverName}</h1>
            {invite.serverDescription && <p className="text-slate-400 mt-2">{invite.serverDescription}</p>}
            <div className="flex items-center justify-center gap-2 text-sm text-slate-400 mt-4">
              <Users size={16} /> {invite.memberCount} members
              {invite.createdBy && <span>· invited by {invite.createdBy.displayName}</span>}
            </div>
            <button onClick={join} disabled={joining || loading} className={`${FormButton.PRIMARY} mt-8 cursor-pointer`}>
              {joining ? "Joining..." : user ? "Accept invite" : "Log in to join"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
