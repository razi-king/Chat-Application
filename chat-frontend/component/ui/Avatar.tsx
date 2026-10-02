import React from "react";
import { cn, initials } from "@/lib/utils";

interface Props {
  name?: string;
  color?: string;
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  online?: boolean;
  showStatus?: boolean;
  square?: boolean;
  className?: string;
}

const sizes = {
  xs: "w-6 h-6 text-[10px]",
  sm: "w-8 h-8 text-xs",
  md: "w-10 h-10 text-sm",
  lg: "w-12 h-12 text-base",
  xl: "w-20 h-20 text-2xl",
};

const dot = {
  xs: "w-2 h-2",
  sm: "w-2.5 h-2.5",
  md: "w-3 h-3",
  lg: "w-3.5 h-3.5",
  xl: "w-5 h-5",
};

// Glowing Gradient Avatar With Initials + Online Dot
const Avatar = ({ name, color = "#22d3ee", size = "md", online, showStatus = false, square = false, className = "" }: Props) => {
  return (
    <div className={cn("relative shrink-0", className)}>
      <div
        className={cn(
          sizes[size],
          square ? "rounded-2xl" : "rounded-full",
          "flex items-center justify-center font-bold text-slate-950 select-none"
        )}
        style={{
          background: `radial-gradient(circle at 30% 25%, #ffffffcc 0%, ${color} 38%, ${color}aa 70%, #0b0d1a 140%)`,
          boxShadow: `0 0 0 1px ${color}55, 0 6px 18px -6px ${color}`,
        }}
      >
        {initials(name)}
      </div>
      {showStatus && (
        <span
          className={cn(
            dot[size],
            "absolute -bottom-0.5 -right-0.5 rounded-full ring-2 ring-[#0a0c19]",
            online ? "bg-emerald-400 shadow-[0_0_8px_#34d399]" : "bg-slate-600"
          )}
        />
      )}
    </div>
  );
};

export default Avatar;
