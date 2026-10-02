"use client";
import React from "react";
import { MessageCircle } from "lucide-react";
import { EmptyState } from "@/component/ui/Primitives";

export default function ChatsHome() {
  return (
    <div className="flex-1 flex items-center justify-center bg-grid">
      <EmptyState
        icon={<MessageCircle size={28} />}
        title="Pick a conversation"
        text="Your DMs and groups live here. Messages, ticks, reactions and typing all update in real time."
      />
    </div>
  );
}
