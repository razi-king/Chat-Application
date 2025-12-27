"use client";
import React from 'react'
import Anchor from './Anchor';

const Navbar = () => {
    const pages = [
        { item: "Home", url: "/" },
        { item: "New Chat", url: "/new-chat" },
        { item: "Join Chat", url: "/join-chat" },
        { item: "About", url: "/about" },
        { item: "Contact", url: "/contact" },
      ];
  return (
    <div className='flex gap-4 justify-evenly p-4 bg-gray-600  text-sm sm:text-xl lg:text-2xl text-white font-bold group:hover:underline-offset-4 transform duration-200'>
       {pages.map((page, index) => (
        <Anchor key={index} href={page.url} label={page.item}/>
      ))}
    </div>
  )
}

export default Navbar