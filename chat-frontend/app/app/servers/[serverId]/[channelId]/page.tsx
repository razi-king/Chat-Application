"use client";
import React from "react";
import { useParams } from "next/navigation";
import ChatPage from "@/component/chat/ChatPage";

export default function ChannelPage() {
  const { serverId, channelId } = useParams<{ serverId: string; channelId: string }>();
  return <ChatPage key={channelId} roomId={channelId} backHref={`/app/servers/${serverId}`} />;
}
