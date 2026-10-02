"use client";
import React from 'react'
import RequireAuth from '@/component/auth/RequireAuth'
import CreateRoomChat from '@/component/chat/CreateRoomChat'

const NewChat = () => {
  return (
    <RequireAuth>
      <div className='min-h-screen bg-aurora flex items-center justify-center px-4 pt-28 pb-12'>
        <div className='w-full max-w-md glass neon-border rounded-3xl p-8'>
          <h1 className='font-display text-3xl font-bold text-white text-center'>Start something new</h1>
          <p className='text-sm text-slate-400 text-center mt-2 mb-8'>Spin up a server for your community or a group for your friends.</p>
          <CreateRoomChat />
        </div>
      </div>
    </RequireAuth>
  )
}

export default NewChat
