import React from "react";
import { cn } from "@/lib/utils";
import UserPanel from "./UserPanel";

// The 2nd Column (Chat List / Channel List / Home Menu) Shared By Every Section
export default function SidebarFrame({
  header,
  children,
  className = "",
}: {
  header: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <aside className={cn("w-full md:w-72 lg:w-80 shrink-0 flex flex-col bg-surface/70 backdrop-blur-xl border-r border-line", className)}>
      <div className="h-16 shrink-0 flex items-center px-4 border-b border-line">{header}</div>
      <div className="flex-1 overflow-y-auto">{children}</div>
      <UserPanel />
    </aside>
  );
}
