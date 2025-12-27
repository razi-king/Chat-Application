"use client";
import React, { useState } from 'react'
import { FormButton } from '../enums/ButtonStyles';
import { MdAttachFile, MdSend } from 'react-icons/md';

const ChatPage = () => {
  const [data, setData] = useState(null);
  return (
    <div>
        {/*This Is Header Section For Chat Template */}
        <header className='flex justify-around top-0 items-center dark:bg-gray-900 py-5 fixed w-full'>
             {/* This Is Room Id */}
            <div>
               {data ? <span>Hello</span>: <span className='text-bold text-2xl '>Family Room</span>}
            </div>
             {/* This Is User Name */}
            <div>
              <h2>Razi</h2>
            </div>
             {/* This Is Delete Button */}
            <div>
              <button className={`${FormButton.LEAVEBUTTON}`}>Leave Room</button>
            </div>
        </header>
        {/* This Is Message Section For Chat Template */}
        <div className='h-screen py-20 border border-gray-600 overflow-auto dark:bg-slate-800 mx-auto w-2/3'>
            <div></div>
            <div></div>
        </div>
        {/* This IS For Sending Message For Chat Template */}
        <div className='bottom-0 fixed  w-full h-16'>
          <div className='w-2/3 px-3 py-2 rounded h-full   mx-auto flex justify-center items-center gap-4'>
            <input type='text'
              name='message'
              className='w-2/3 focus:outline-none px-5 border border-gray-500 h-full'
              placeholder='Type Your Message Here '
            />
            <div className='flex gap-4'>
              <button className='bg-purple-600  cursor-pointer rounded-full w-12 h-12 flex justify-center items-center'> 
                <MdAttachFile size={20} />
              </button>
              <button className='bg-green-600 cursor-pointer rounded-full w-12 h-12 flex justify-center items-center'> 
                <MdSend size={20} />
              </button>
            </div>
          </div>

        </div>
    </div>
  )
}

export default ChatPage