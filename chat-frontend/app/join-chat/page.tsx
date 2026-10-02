"use client";
import React from 'react'
import RequireAuth from '@/component/auth/RequireAuth'
import JoinRoomChat from '@/component/chat/JoinRoomChat'

const JoinChat = () => {
  return (
    <RequireAuth>
      <div className='bg-aurora pt-16'>
        <JoinRoomChat fullScreen />
      </div>
    </RequireAuth>
  )
}

export default JoinChat
