"use client";
import React from "react";
import dynamic from "next/dynamic";
import type { SceneVariant } from "./NexusScene";

// Shown While three.js Loads, And If WebGL Is Not Available
const Fallback = () => (
  <div className="absolute inset-0 flex items-center justify-center">
    <div className="w-64 h-64 rounded-full bg-[radial-gradient(circle_at_30%_30%,#67e8f9,#0e7490_45%,transparent_70%)] blur-2xl opacity-70 animate-glow" />
  </div>
);

// WebGL Only Exists In The Browser -> Never Render On The Server
const NexusScene = dynamic(() => import("./NexusScene"), { ssr: false, loading: () => <Fallback /> });

// A Crash Inside The 3D Scene (Old GPU, WebGL Disabled) Must Never Break The Page
class SceneBoundary extends React.Component<{ children: React.ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  componentDidCatch(error: unknown) {
    console.warn("3D scene disabled:", error);
  }
  render() {
    return this.state.failed ? <Fallback /> : this.props.children;
  }
}

export default function Scene3D({ variant = "hero", className = "" }: { variant?: SceneVariant; className?: string }) {
  return (
    <div className={`absolute inset-0 ${className}`}>
      <SceneBoundary>
        <NexusScene variant={variant} />
      </SceneBoundary>
    </div>
  );
}
