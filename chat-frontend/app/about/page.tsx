import React from 'react'
import type { Metadata } from 'next'
import { Database, Layers, Radio, ShieldCheck } from 'lucide-react'

export const metadata: Metadata = { title: "About" }

const layers = [
  { icon: Layers, title: "Frontend", text: "Next.js 16 App Router, React 19, Tailwind 4, Three.js / React Three Fiber for 3D, Formik + Yup forms, STOMP client for realtime." },
  { icon: ShieldCheck, title: "Backend", text: "Spring Boot 3 (Java 17): Controller → Service interface → Impl → Repository, DTOs for every request/response, JWT security and a custom exception hierarchy." },
  { icon: Database, title: "MongoDB (10 collections)", text: "users, servers, server_members, rooms, room_members, messages, invites, friendships, notifications, audit_logs." },
  { icon: Radio, title: "Redis + WebSocket", text: "Presence, unread counters, rate limiting and token blacklist in Redis. STOMP topics for rooms, presence and private user queues." },
]

const About = () => {
  return (
    <div className='min-h-screen bg-aurora pt-32 pb-20 px-4'>
      <div className='max-w-4xl mx-auto'>
        <p className="font-mono text-xs uppercase tracking-[0.3em] text-cyan-300">About the project</p>
        <h1 className='font-display text-5xl font-bold text-white mt-3'>Nexus is WhatsApp <span className='text-gradient'>meets</span> Discord.</h1>
        <p className='text-lg text-slate-400 mt-6 leading-relaxed'>
          A full-stack real-time chat platform built as a final-year project by Razi. Private chats and groups feel like
          WhatsApp (read ticks, last seen, replies), while servers feel like Discord (channels, roles, invites, audit logs).
        </p>
        <div className='grid sm:grid-cols-2 gap-5 mt-12'>
          {layers.map((l) => (
            <div key={l.title} className='glass rounded-2xl p-6'>
              <l.icon className='text-cyan-300' />
              <h3 className='font-display text-lg font-semibold text-white mt-4'>{l.title}</h3>
              <p className='text-sm text-slate-400 mt-2 leading-relaxed'>{l.text}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default About
