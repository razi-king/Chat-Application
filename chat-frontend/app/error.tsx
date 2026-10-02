"use client";
import { useEffect } from "react";
import Link from "next/link";
import { TriangleAlert } from "lucide-react";
import { FormButton } from "@/component/enums/ButtonStyles";
import { getErrorMessage } from "@/lib/errorHandler";

// Catches Render Crashes In Any Page (Our Last Line Of The Frontend Exception Handling)
export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="min-h-screen bg-aurora flex items-center justify-center p-4">
      <div className="max-w-md w-full glass neon-border rounded-3xl p-8 text-center">
        <div className="w-14 h-14 mx-auto rounded-2xl bg-rose-500/15 text-rose-300 flex items-center justify-center">
          <TriangleAlert />
        </div>
        <h1 className="font-display text-2xl font-bold text-white mt-5">Something glitched</h1>
        <p className="text-slate-400 mt-2 text-sm">{getErrorMessage(error)}</p>
        {error.digest && <p className="font-mono text-[11px] text-slate-600 mt-2">ref: {error.digest}</p>}
        <div className="flex gap-3 justify-center mt-6">
          <button onClick={reset} className={`${FormButton.PRIMARY} !w-auto cursor-pointer`}>Try again</button>
          <Link href="/" className={FormButton.SECONDARY}>Home</Link>
        </div>
      </div>
    </div>
  );
}
