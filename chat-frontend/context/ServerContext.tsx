"use client";
import React, { createContext, useContext } from "react";
import type { ServerDetail } from "@/types";

export interface ServerContextValue {
  detail: ServerDetail;
  reload: () => Promise<void>;
}

// Provided By /app/servers/[serverId]/layout.tsx, null Everywhere Else (DMs / Groups)
export const ServerContext = createContext<ServerContextValue | null>(null);

export function useServerContext(): ServerContextValue | null {
  return useContext(ServerContext);
}

export function ServerProvider({ value, children }: { value: ServerContextValue; children: React.ReactNode }) {
  return <ServerContext.Provider value={value}>{children}</ServerContext.Provider>;
}
