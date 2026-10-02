"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Hash } from "lucide-react";
import { EmptyState } from "@/component/ui/Primitives";
import { useServerContext } from "@/context/ServerContext";

// Opening A Server Jumps Into Its First Channel (Desktop). Phones See The Channel List.
export default function ServerHome() {
  const ctx = useServerContext();
  const router = useRouter();
  const first = ctx?.detail.channels[0];

  useEffect(() => {
    if (first && window.innerWidth >= 768) router.replace(`/app/servers/${first.serverId}/${first.id}`);
  }, [first, router]);

  return (
    <div className="flex-1 flex items-center justify-center bg-grid">
      <EmptyState icon={<Hash size={26} />} title={ctx?.detail.server.name ?? "Server"} text="Pick a channel to start talking." />
    </div>
  );
}
