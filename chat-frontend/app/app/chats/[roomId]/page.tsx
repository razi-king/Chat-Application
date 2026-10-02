"use client";
import React from "react";
import { useParams } from "next/navigation";
import ChatPage from "@/component/chat/ChatPage";

export default function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  return <ChatPage key={roomId} roomId={roomId} backHref="/app/chats" />;
}
