"use client";
import React, { Suspense, useEffect } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import toast from "react-hot-toast";
import AuthShell from "@/component/auth/AuthShell";
import ReusableForm from "@/component/form/ReusableForm";
import { registerSchema } from "@/component/form/formSchema";
import { FormStyles } from "@/component/enums/FormStyles";
import { useAuth } from "@/context/AuthContext";
import { handleError } from "@/lib/errorHandler";

function RegisterForm() {
  const { register, user, loading } = useAuth();
  const router = useRouter();
  const next = useSearchParams().get("next") || "/app";

  useEffect(() => {
    if (!loading && user) router.replace(next);
  }, [loading, user, next, router]);

  return (
    <ReusableForm
      fullScreen={false}
      initialValues={{ displayName: "", username: "", email: "", password: "", confirmPassword: "" }}
      validationSchema={registerSchema}
      formTitle="Join the Nexus"
      formSubtitle="One account for chats, groups and servers"
      buttonText="Create account"
      formStyle={FormStyles.AUTHFORM}
      fields={[
        { name: "displayName", label: "Display name", placeHolder: "Razi Khan" },
        { name: "username", label: "Username", placeHolder: "razi" },
        { name: "email", label: "Email", type: "email", placeHolder: "you@example.com" },
        { name: "password", label: "Password", type: "password", placeHolder: "At least 6 characters" },
        { name: "confirmPassword", label: "Confirm password", type: "password", placeHolder: "Repeat password" },
      ]}
      onSubmit={async ({ confirmPassword: _confirm, ...values }, actions) => {
        try {
          const u = await register(values);
          toast.success(`Account created. Welcome, ${u.displayName}!`);
          router.replace(next);
        } catch (e) {
          // USER_409 / USER_410 Come With errorMeta -> Shown Under Username / Email
          handleError(e, { setFieldErrors: actions.setErrors });
        } finally {
          actions.setSubmitting(false);
        }
      }}
      footer={
        <p className="text-center text-sm text-slate-400">
          Already have an account?{" "}
          <Link href="/login" className="text-cyan-300 hover:text-cyan-200 font-medium">Log in</Link>
        </p>
      }
    />
  );
}

export default function RegisterPage() {
  return (
    <AuthShell tagline="Build your network. Start your first server in seconds.">
      <Suspense>
        <RegisterForm />
      </Suspense>
    </AuthShell>
  );
}
