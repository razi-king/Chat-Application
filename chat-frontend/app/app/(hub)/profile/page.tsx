"use client";
import React, { useState } from "react";
import toast from "react-hot-toast";
import { LogOut } from "lucide-react";
import Avatar from "@/component/ui/Avatar";
import { TiltCard } from "@/component/ui/Primitives";
import ReusableForm from "@/component/form/ReusableForm";
import { FormStyles } from "@/component/enums/FormStyles";
import { FormButton } from "@/component/enums/ButtonStyles";
import { profileSchema } from "@/component/form/formSchema";
import { UserService } from "@/services/AuthService";
import { useAuth } from "@/context/AuthContext";
import { handleError } from "@/lib/errorHandler";
import { cn, formatDay, PALETTE } from "@/lib/utils";

export default function ProfilePage() {
  const { user, setUser, logout } = useAuth();
  const [color, setColor] = useState(user?.avatarColor ?? PALETTE[0]);
  if (!user) return null;

  return (
    <div className="p-6 lg:p-10 max-w-5xl grid lg:grid-cols-[320px_1fr] gap-8">
      {/* Live Preview Card */}
      <TiltCard className="glass neon-border rounded-3xl overflow-hidden h-fit">
        <div className="h-28" style={{ background: `linear-gradient(135deg, ${color}aa, transparent), radial-gradient(circle at 80% 0%, ${color}66, transparent 60%)` }} />
        <div className="px-6 pb-6 -mt-10">
          <Avatar name={user.displayName} color={color} size="xl" showStatus online />
          <h2 className="font-display text-2xl font-bold text-white mt-3">{user.displayName}</h2>
          <p className="text-sm text-slate-500">@{user.username}</p>
          {user.customStatus && <p className="mt-3 text-sm text-slate-300 glass-soft rounded-xl px-3 py-2">{user.customStatus}</p>}
          <div className="mt-4 text-[11px] uppercase tracking-widest text-slate-500">About</div>
          <p className="text-sm text-slate-300 mt-1">{user.about || "—"}</p>
          {user.createdAt && <p className="text-xs text-slate-500 mt-4">Member since {formatDay(user.createdAt)}</p>}
        </div>
      </TiltCard>

      <div className="glass rounded-3xl p-6 lg:p-8">
        <h1 className="font-display text-2xl font-bold text-white">Edit profile</h1>
        <p className="text-sm text-slate-500 mt-1 mb-6">{user.email}</p>
        <p className="text-xs uppercase tracking-[0.14em] text-slate-400 mb-2">Avatar color</p>
        <div className="flex flex-wrap gap-2 mb-6">
          {PALETTE.map((c) => (
            <button key={c} onClick={() => setColor(c)} aria-label={c}
              className={cn("w-9 h-9 rounded-full transition-transform cursor-pointer", color === c && "scale-110 ring-2 ring-white")}
              style={{ background: c, boxShadow: `0 0 16px -4px ${c}` }} />
          ))}
        </div>
        <ReusableForm
          fullScreen={false}
          formTitle=""
          formStyle={FormStyles.MODALFORM}
          enableReinitialize
          initialValues={{ displayName: user.displayName, about: user.about ?? "", customStatus: user.customStatus ?? "" }}
          validationSchema={profileSchema}
          buttonText="Save changes"
          fields={[
            { name: "displayName", label: "Display name" },
            { name: "customStatus", label: "Status", placeHolder: "🎧 Coding late tonight" },
            { name: "about", label: "About", type: "textarea", rows: 3 },
          ]}
          onSubmit={async (values, actions) => {
            try {
              const updated = await UserService.updateMe({ ...values, avatarColor: color });
              setUser(updated);
              toast.success("Profile updated");
            } catch (e) {
              handleError(e, { setFieldErrors: actions.setErrors });
            } finally {
              actions.setSubmitting(false);
            }
          }}
        />
        <button onClick={logout} className={`${FormButton.LEAVEBUTTON} mt-8 flex items-center gap-2 cursor-pointer`}><LogOut size={15} /> Log out</button>
      </div>
    </div>
  );
}
