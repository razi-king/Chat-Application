"use client";
import React from "react";
import RequireAuth from "@/component/auth/RequireAuth";
import { SocketProvider } from "@/context/SocketContext";
import { AppStateProvider } from "@/context/AppStateContext";
import { useAuth } from "@/context/AuthContext";
import ServerRail from "@/component/layout/ServerRail";
import MobileNav from "@/component/layout/MobileNav";

function Shell({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  return (
    <SocketProvider token={token}>
      <AppStateProvider>
        {/* Desktop: Rail On The Left. Phone: Content On Top, Tab Bar At The Bottom */}
        <div className="h-dvh flex flex-col md:flex-row bg-aurora overflow-hidden text-slate-200">
          <ServerRail />
          <div className="flex-1 flex min-w-0 min-h-0">{children}</div>
          <MobileNav />
        </div>
      </AppStateProvider>
    </SocketProvider>
  );
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth>
      <Shell>{children}</Shell>
    </RequireAuth>
  );
}
