import React from 'react'
import type { Metadata } from 'next'
import { Mail, MessageCircle } from 'lucide-react'
import { FaGithub } from 'react-icons/fa'

export const metadata: Metadata = { title: "Contact" }

const Contact = () => {
  return (
    <div className='min-h-screen bg-aurora pt-32 pb-20 px-4 flex items-start justify-center'>
      <div className='max-w-xl w-full glass neon-border rounded-3xl p-10 text-center'>
        <div className='w-14 h-14 mx-auto rounded-2xl glass flex items-center justify-center text-cyan-300 glow-cyan'>
          <MessageCircle />
        </div>
        <h1 className='font-display text-4xl font-bold text-white mt-6'>Get in touch</h1>
        <p className='text-slate-400 mt-3'>Questions, feedback or found a bug? Reach the developer directly.</p>
        <div className='mt-8 flex flex-col gap-3'>
          <a href="mailto:contact@example.com" className='glass-soft rounded-xl px-5 py-4 flex items-center gap-3 text-slate-200 hover:border-cyan-400/40 transition-colors'>
            <Mail size={18} className='text-cyan-300' /> contact@example.com
          </a>
          <a href="https://github.com" target="_blank" rel="noopener noreferrer" className='glass-soft rounded-xl px-5 py-4 flex items-center gap-3 text-slate-200 hover:border-violet-400/40 transition-colors'>
            <FaGithub size={18} className='text-violet-300' /> github.com
          </a>
        </div>
      </div>
    </div>
  )
}

export default Contact
