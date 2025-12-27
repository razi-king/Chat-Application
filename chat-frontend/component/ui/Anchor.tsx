import React from "react";

interface Props {
  href: string;
  label: string;
  className?: string;
}

const Anchor = ({ href, label, className = "" }: Props) => {
  return (
    <a
      href={href}
      className={`
        relative
        after:content-['']
        after:absolute
        after:left-0
        after:-bottom-1
        after:h-[2px]
        after:w-0
        after:bg-white
        after:transition-all
        after:duration-300
        after:delay-150
        hover:after:w-full
        ${className}
      `}
    >
      {label}
    </a>
  );
};

export default Anchor;
