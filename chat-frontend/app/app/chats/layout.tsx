"use client";
import React from "react";
import { useParams } from "next/navigation";
import ConversationList from "@/component/layout/ConversationList";
import { cn } from "@/lib/utils";

export default function ChatsLayout({ children }: { children: React.ReactNode }) {
  const { roomId } = useParams<{ roomId?: string }>();
  return (
    <>
      {/* On Phones Show Either The List Or The Open Chat */}
      <ConversationList className={cn(roomId ? "hidden md:flex" : "flex")} />
      <main className={cn("flex-1 min-w-0", roomId ? "flex" : "hidden md:flex")}>{children}</main>
    </>
  );
}
