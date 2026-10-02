"use client";
import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import toast from 'react-hot-toast'
import { Check, Hash, Server as ServerIcon, Users } from 'lucide-react'
import ReusableForm from '../form/ReusableForm'
import { FormStyles } from '../enums/FormStyles'
import { createGroupSchema, createServerSchema } from '../form/formSchema'
import Avatar from '../ui/Avatar'
import { ServerService } from '@/services/ServerService'
import { RoomService } from '@/services/RoomService'
import { FriendService } from '@/services/SocialService'
import { handleError } from '@/lib/errorHandler'
import { cn, PALETTE } from '@/lib/utils'
import type { User } from '@/types'

type Mode = "server" | "group";

interface Props {
  defaultMode?: Mode;
  // Called With The Route To Open After Creation (Defaults To Navigating There)
  onDone?: (path: string) => void;
}

// Create A Discord Style Server Or A WhatsApp Style Group
const CreateRoomChat = ({ defaultMode = "server", onDone }: Props) => {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>(defaultMode);
  const [color, setColor] = useState(PALETTE[0]);
  const [friends, setFriends] = useState<User[]>([]);
  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    if (mode !== "group") return;
    FriendService.list()
      .then((list) => setFriends(list.filter((f) => f.status === "ACCEPTED").map((f) => f.user)))
      .catch((e) => handleError(e));
  }, [mode]);

  const finish = (path: string) => (onDone ? onDone(path) : router.push(path));
  const toggle = (id: string) => setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));

  return (
    <div>
      <div className='grid grid-cols-2 gap-2 p-1 rounded-xl bg-white/[0.03] border border-line mb-6'>
        {([["server", "Server", ServerIcon], ["group", "Group chat", Users]] as const).map(([m, label, Icon]) => (
          <button
            key={m}
            type="button"
            onClick={() => setMode(m)}
            className={cn(
              'flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-medium transition-all cursor-pointer',
              mode === m ? 'bg-gradient-to-r from-cyan-400/20 to-violet-400/20 text-white shadow-inner' : 'text-slate-400 hover:text-white'
            )}
          >
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      {mode === "server" ? (
        <>
          <p className='text-xs uppercase tracking-[0.14em] text-slate-400 mb-2'>Server color</p>
          <div className='flex flex-wrap gap-2 mb-5'>
            {PALETTE.map((c) => (
              <button key={c} type="button" onClick={() => setColor(c)} aria-label={c}
                className={cn('w-8 h-8 rounded-xl transition-transform cursor-pointer', color === c && 'scale-110 ring-2 ring-white/80')}
                style={{ background: c, boxShadow: `0 0 16px -4px ${c}` }} />
            ))}
          </div>
          <ReusableForm
            fullScreen={false}
            formTitle=""
            formStyle={FormStyles.MODALFORM}
            initialValues={{ name: "", description: "", visibility: "private" }}
            validationSchema={createServerSchema}
            buttonText="Create server"
            fields={[
              { name: "name", label: "Server name", placeHolder: "My awesome server" },
              { name: "description", label: "Description", type: "textarea", rows: 2, placeHolder: "What is this server about?" },
              { name: "visibility", label: "Visibility", type: "select", options: [
                { label: "Invite only", value: "private" },
                { label: "Public (listed in Discover)", value: "public" },
              ] },
            ]}
            onSubmit={async (values, actions) => {
              try {
                const server = await ServerService.create({
                  name: values.name, description: values.description, iconColor: color, discoverable: values.visibility === "public",
                });
                toast.success(`${server.name} is live!`);
                finish(`/app/servers/${server.id}`);
              } catch (e) {
                handleError(e, { setFieldErrors: actions.setErrors });
              } finally {
                actions.setSubmitting(false);
              }
            }}
          />
        </>
      ) : (
        <>
          <p className='text-xs uppercase tracking-[0.14em] text-slate-400 mb-2'>Add friends ({selected.length})</p>
          <div className='max-h-44 overflow-y-auto space-y-1 mb-5 pr-1'>
            {friends.length === 0 && <p className='text-sm text-slate-500'>Add some friends first to create a group.</p>}
            {friends.map((f) => (
              <button key={f.id} type="button" onClick={() => toggle(f.id)}
                className={cn('w-full flex items-center gap-3 px-3 py-2 rounded-xl transition-colors cursor-pointer',
                  selected.includes(f.id) ? 'bg-cyan-400/10' : 'hover:bg-white/5')}>
                <Avatar name={f.displayName} color={f.avatarColor} size="sm" />
                <span className='text-sm text-slate-200 flex-1 text-left'>{f.displayName}</span>
                <span className={cn('w-5 h-5 rounded-md border flex items-center justify-center',
                  selected.includes(f.id) ? 'bg-cyan-400 border-cyan-400 text-slate-950' : 'border-white/20')}>
                  {selected.includes(f.id) && <Check size={14} />}
                </span>
              </button>
            ))}
          </div>
          <ReusableForm
            fullScreen={false}
            formTitle=""
            formStyle={FormStyles.MODALFORM}
            initialValues={{ name: "", description: "" }}
            validationSchema={createGroupSchema}
            buttonText="Create group"
            fields={[
              { name: "name", label: "Group name", placeHolder: "Exam Squad" },
              { name: "description", label: "Description", placeHolder: "Optional" },
            ]}
            onSubmit={async (values, actions) => {
              if (selected.length === 0) {
                toast.error("Pick at least one friend");
                actions.setSubmitting(false);
                return;
              }
              try {
                const room = await RoomService.createGroup({ ...values, memberIds: selected });
                toast.success("Group created");
                finish(`/app/chats/${room.id}`);
              } catch (e) {
                handleError(e, { setFieldErrors: actions.setErrors });
              } finally {
                actions.setSubmitting(false);
              }
            }}
          />
        </>
      )}
      <p className='mt-4 text-xs text-slate-500 flex items-center gap-1.5'>
        <Hash size={12} /> Servers start with #general, #announcements and #off-topic.
      </p>
    </div>
  )
}

export default CreateRoomChat
