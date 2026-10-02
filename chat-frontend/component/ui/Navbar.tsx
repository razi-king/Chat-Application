"use client";
import React, { useState } from 'react'
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Menu, X } from 'lucide-react';
import Anchor from './Anchor';
import { Logo } from './Primitives';
import { useAuth } from '@/context/AuthContext';
import { FormButton } from '../enums/ButtonStyles';

// Marketing Navbar. The Chat App (/app) And Auth Pages Have Their Own Full Screen Layout.
const HIDDEN_ON = ["/app", "/login", "/register", "/invite"];

const Navbar = () => {
    const pathname = usePathname();
    const { user } = useAuth();
    const [open, setOpen] = useState(false);
    const pages = [
        { item: "Home", url: "/" },
        { item: "New Chat", url: "/new-chat" },
        { item: "Join Chat", url: "/join-chat" },
        { item: "About", url: "/about" },
        { item: "Contact", url: "/contact" },
      ];
    if (HIDDEN_ON.some((p) => pathname === p || pathname.startsWith(p + "/"))) return null;
  return (
    <header className='fixed top-0 inset-x-0 z-40'>
      <div className='mx-auto max-w-6xl mt-4 px-4'>
        <nav className='glass rounded-2xl px-5 py-3 flex items-center justify-between'>
          <Link href="/"><Logo size={28} /></Link>
          <div className='hidden md:flex gap-8 text-sm font-medium text-slate-300'>
            {pages.map((page) => (
              <Anchor key={page.url} href={page.url} label={page.item} active={pathname === page.url} />
            ))}
          </div>
          <div className='hidden md:flex items-center gap-3'>
            {user ? (
              <Link href="/app" className={`${FormButton.PRIMARY} !w-auto !py-2 text-sm`}>Open Nexus</Link>
            ) : (
              <>
                <Link href="/login" className={`${FormButton.GHOST} text-sm`}>Log in</Link>
                <Link href="/register" className={`${FormButton.PRIMARY} !w-auto !py-2 text-sm`}>Get started</Link>
              </>
            )}
          </div>
          <button className='md:hidden text-slate-200' onClick={() => setOpen((o) => !o)} aria-label="Menu">
            {open ? <X /> : <Menu />}
          </button>
        </nav>
        {open && (
          <div className='md:hidden glass rounded-2xl mt-2 p-4 flex flex-col gap-3 text-slate-200'>
            {pages.map((page) => (
              <Link key={page.url} href={page.url} onClick={() => setOpen(false)}>{page.item}</Link>
            ))}
            <Link href={user ? "/app" : "/login"} onClick={() => setOpen(false)} className={`${FormButton.PRIMARY} text-center`}>
              {user ? "Open Nexus" : "Log in"}
            </Link>
          </div>
        )}
      </div>
    </header>
  )
}

export default Navbar
