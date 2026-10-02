"use client";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ServerCrash } from "lucide-react";
import ChannelSidebar from "@/component/layout/ChannelSidebar";
import { EmptyState, Spinner } from "@/component/ui/Primitives";
import { FormButton } from "@/component/enums/ButtonStyles";
import { ServerProvider } from "@/context/ServerContext";
import { useAppState } from "@/context/AppStateContext";
import { ServerService } from "@/services/ServerService";
import { getErrorMessage, handleError } from "@/lib/errorHandler";
import { cn } from "@/lib/utils";
import type { ServerDetail } from "@/types";

export default function ServerLayout({ children }: { children: React.ReactNode }) {
  const { serverId, channelId } = useParams<{ serverId: string; channelId?: string }>();
  const { serverVersion } = useAppState();
  const [detail, setDetail] = useState<ServerDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const version = serverVersion[serverId] ?? 0;

  const reload = useCallback(async () => {
    try {
      setDetail(await ServerService.detail(serverId));
      setError(null);
    } catch (e) {
      setError(getErrorMessage(handleError(e, { silent: true })));
    }
  }, [serverId]);

  // Re-Fetch When Members / Channels / Unread Counts Change (Pushed Over WebSocket)
  useEffect(() => {
    // Data Fetch: State Is Only Set After The Request Resolves
    // eslint-disable-next-line react-hooks/set-state-in-effect
    reload();
  }, [reload, version]);

  const ctx = useMemo(() => (detail ? { detail, reload } : null), [detail, reload]);

  if (error) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <EmptyState icon={<ServerCrash size={26} />} title="Server unavailable" text={error}
          action={<Link href="/app" className={`${FormButton.SECONDARY} mt-2`}>Back home</Link>} />
      </div>
    );
  }
  if (!ctx) return <div className="flex-1 flex items-center justify-center"><Spinner size={32} /></div>;

  return (
    <ServerProvider value={ctx}>
      <ChannelSidebar detail={ctx.detail} reload={reload} className={cn(channelId ? "hidden md:flex" : "flex")} />
      <main className={cn("flex-1 min-w-0", channelId ? "flex" : "hidden md:flex")}>{children}</main>
    </ServerProvider>
  );
}
