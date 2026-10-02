import React from "react";
import Link from "next/link";

interface Props {
  href: string;
  label: string;
  className?: string;
  active?: boolean;
}

// next/link -> Client Side Navigation Instead Of A Full Page Reload
const Anchor = ({ href, label, className = "", active = false }: Props) => {
  return (
    <Link
      href={href}
      className={`
        relative
        after:content-['']
        after:absolute
        after:left-0
        after:-bottom-1
        after:h-[2px]
        after:bg-gradient-to-r
        after:from-cyan-400
        after:to-violet-400
        after:transition-all
        after:duration-300
        hover:after:w-full
        hover:text-white
        transition-colors
        ${active ? "after:w-full text-white" : "after:w-0"}
        ${className}
      `}
    >
      {label}
    </Link>
  );
};

export default Anchor;
