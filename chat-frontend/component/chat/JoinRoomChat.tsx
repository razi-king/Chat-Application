"use client";
import React from 'react'
import { useRouter } from 'next/navigation'
import toast from 'react-hot-toast'
import ReusableForm from '../form/ReusableForm'
import { FormStyles } from '../enums/FormStyles'
import { joinRoomSchema } from '../form/formSchema'
import { InviteService } from '@/services/ServerService'
import { handleError } from '@/lib/errorHandler'

interface Props {
  fullScreen?: boolean;
  onDone?: (path: string) => void;
}

// "https://.../invite/NEXUSHQ1" Or Just "NEXUSHQ1"
const extractCode = (input: string) => input.trim().split("/").filter(Boolean).pop() ?? "";

// Join A Server With An Invite Code Or Link
const JoinRoomChat = ({ fullScreen = false, onDone }: Props) => {
    const router = useRouter();
    const initialValues = { roomId: '' }
    const field = [
        { name: 'roomId', label: 'Invite code or link', placeHolder: 'NEXUSHQ1 or http://localhost:3000/invite/NEXUSHQ1' },
    ]
  return (
    <ReusableForm
      fullScreen={fullScreen}
      initialValues={initialValues}
      onSubmit={async (values, actions) => {
        try {
          const server = await InviteService.accept(extractCode(values.roomId));
          toast.success(`Joined ${server.name}`);
          const path = `/app/servers/${server.id}`;
          if (onDone) onDone(path); else router.push(path);
        } catch (e) {
          handleError(e);
          actions.setFieldError('roomId', 'This invite is invalid or expired');
        } finally {
          actions.setSubmitting(false);
        }
      }}
      validationSchema={joinRoomSchema}
      fields={field}
      formTitle={fullScreen ? 'Join a server' : ''}
      formSubtitle="Paste an invite you received from a friend"
      buttonText='Join'
      formStyle={fullScreen ? FormStyles.JOINROOMFORM : FormStyles.MODALFORM}
    />
  )
}

export default JoinRoomChat
