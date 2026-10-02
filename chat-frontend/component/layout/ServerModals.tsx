"use client";
import React, { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Copy, Link2, ScrollText } from "lucide-react";
import Modal from "@/component/ui/Modal";
import Avatar from "@/component/ui/Avatar";
import { Spinner } from "@/component/ui/Primitives";
import ReusableForm from "@/component/form/ReusableForm";
import { FormStyles } from "@/component/enums/FormStyles";
import { FormButton } from "@/component/enums/ButtonStyles";
import { createChannelSchema } from "@/component/form/formSchema";
import { ServerService } from "@/services/ServerService";
import { handleError } from "@/lib/errorHandler";
import { timeAgo } from "@/lib/utils";
import type { AuditLog, Invite, Room } from "@/types";

export function CreateChannelModal({ serverId, open, onClose, onCreated }: { serverId: string; open: boolean; onClose: () => void; onCreated: (room: Room) => void }) {
  return (
    <Modal open={open} onClose={onClose} title="Create channel" subtitle="Text channels are where your server talks">
      <ReusableForm
        fullScreen={false}
        formTitle=""
        formStyle={FormStyles.MODALFORM}
        initialValues={{ name: "", description: "" }}
        validationSchema={createChannelSchema}
        buttonText="Create channel"
        fields={[
          { name: "name", label: "Channel name", placeHolder: "project-ideas" },
          { name: "description", label: "Topic", placeHolder: "What is this channel for?" },
        ]}
        onSubmit={async (values, actions) => {
          try {
            const room = await ServerService.createChannel(serverId, { ...values, name: values.name.trim().toLowerCase() });
            toast.success(`#${room.name} created`);
            onCreated(room);
          } catch (e) {
            handleError(e, { setFieldErrors: actions.setErrors });
          } finally {
            actions.setSubmitting(false);
          }
        }}
      />
    </Modal>
  );
}

export function InviteModal({ serverId, serverName, open, onClose }: { serverId: string; serverName: string; open: boolean; onClose: () => void }) {
  const [invite, setInvite] = useState<Invite | null>(null);
  const [expires, setExpires] = useState(24);
  const [loading, setLoading] = useState(false);

  const create = async (hours: number) => {
    setLoading(true);
    try {
      setInvite(await ServerService.createInvite(serverId, { maxUses: 0, expiresInHours: hours }));
    } catch (e) {
      handleError(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) create(24);
    else setInvite(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const link = invite ? `${window.location.origin}/invite/${invite.code}` : "";
  const copy = () => {
    navigator.clipboard?.writeText(link);
    toast.success("Invite link copied");
  };

  return (
    <Modal open={open} onClose={onClose} title={`Invite friends to ${serverName}`} subtitle="Share this link. Anyone with it can join.">
      <div className="glass-soft rounded-xl p-3 flex items-center gap-2">
        <Link2 size={16} className="text-cyan-300 shrink-0" />
        {loading || !invite ? <Spinner size={16} /> : <code className="text-sm text-white truncate flex-1">{link}</code>}
        <button onClick={copy} disabled={!invite} className={`${FormButton.PRIMARY} !w-auto !py-1.5 !px-3 text-sm flex items-center gap-1 cursor-pointer`}>
          <Copy size={14} /> Copy
        </button>
      </div>
      {invite && <p className="text-xs text-slate-500 mt-2">Code: <span className="font-mono text-slate-300">{invite.code}</span></p>}
      <div className="mt-5">
        <label className="text-xs uppercase tracking-[0.14em] text-slate-400">Expire after</label>
        <select
          value={expires}
          onChange={(e) => {
            const h = Number(e.target.value);
            setExpires(h);
            create(h);
          }}
          className="mt-2 w-full rounded-xl bg-white/[0.04] border border-line px-3 py-2 text-sm outline-none"
        >
          <option value={1}>1 hour</option>
          <option value={24}>1 day</option>
          <option value={168}>7 days</option>
          <option value={0}>Never</option>
        </select>
      </div>
    </Modal>
  );
}

const actionText: Record<string, string> = {
  SERVER_CREATED: "created the server",
  SERVER_UPDATED: "updated the server",
  CHANNEL_CREATED: "created channel",
  CHANNEL_DELETED: "deleted channel",
  MEMBER_JOINED: "joined the server",
  MEMBER_LEFT: "left the server",
  MEMBER_KICKED: "kicked",
  ROLE_CHANGED: "changed a role to",
  INVITE_CREATED: "created invite",
  MESSAGE_PINNED: "pinned a message in",
};

export function AuditLogModal({ serverId, open, onClose }: { serverId: string; open: boolean; onClose: () => void }) {
  const [logs, setLogs] = useState<AuditLog[] | null>(null);
  useEffect(() => {
    if (!open) return;
    ServerService.auditLogs(serverId).then((p) => setLogs(p.items)).catch((e) => { handleError(e); setLogs([]); });
  }, [open, serverId]);
  return (
    <Modal open={open} onClose={onClose} title="Audit log" subtitle="Every important action in this server" width="max-w-lg">
      {logs === null && <div className="flex justify-center py-8"><Spinner /></div>}
      {logs?.length === 0 && <p className="text-center text-sm text-slate-500 py-6"><ScrollText className="mx-auto mb-2 opacity-50" />Nothing yet</p>}
      <div className="space-y-1">
        {logs?.map((l) => (
          <div key={l.id} className="flex items-start gap-3 px-2 py-2 rounded-xl hover:bg-white/5">
            <Avatar name={l.actor?.displayName} color={l.actor?.avatarColor} size="sm" />
            <div className="text-sm min-w-0">
              <span className="font-semibold text-white">{l.actor?.displayName ?? "Someone"}</span>{" "}
              <span className="text-slate-400">{actionText[l.action] ?? l.action.toLowerCase()}</span>{" "}
              {l.details && <span className="text-cyan-300">{l.details}</span>}
              <div className="text-[11px] text-slate-500">{timeAgo(l.createdAt)}</div>
            </div>
          </div>
        ))}
      </div>
    </Modal>
  );
}
