"use client";
import React, { Suspense, useEffect } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import toast from "react-hot-toast";
import AuthShell from "@/component/auth/AuthShell";
import ReusableForm from "@/component/form/ReusableForm";
import { loginSchema } from "@/component/form/formSchema";
import { FormStyles } from "@/component/enums/FormStyles";
import { useAuth } from "@/context/AuthContext";
import { handleError } from "@/lib/errorHandler";
import { ErrorCodes, isApiError } from "@/lib/ApiError";

function LoginForm() {
  const { login, user, loading } = useAuth();
  const router = useRouter();
  const next = useSearchParams().get("next") || "/app";

  useEffect(() => {
    if (!loading && user) router.replace(next);
  }, [loading, user, next, router]);

  return (
    <ReusableForm
      fullScreen={false}
      initialValues={{ identifier: "", password: "" }}
      validationSchema={loginSchema}
      formTitle="Welcome back"
      formSubtitle="Log in to jump back into your conversations"
      buttonText="Log in"
      formStyle={FormStyles.AUTHFORM}
      fields={[
        { name: "identifier", label: "Username or email", placeHolder: "razi or razi@nexus.dev" },
        { name: "password", label: "Password", type: "password", placeHolder: "••••••••" },
      ]}
      onSubmit={async (values, actions) => {
        try {
          const u = await login(values.identifier, values.password);
          toast.success(`Welcome back, ${u.displayName.split(" ")[0]}!`);
          router.replace(next);
        } catch (e) {
          handleError(e, { setFieldErrors: actions.setErrors });
          if (isApiError(e) && e.code === ErrorCodes.INVALID_CREDENTIALS) {
            actions.setFieldError("password", "Invalid username or password");
          }
        } finally {
          actions.setSubmitting(false);
        }
      }}
      footer={
        <p className="text-center text-sm text-slate-400">
          New to Nexus?{" "}
          <Link href={`/register${next !== "/app" ? `?next=${encodeURIComponent(next)}` : ""}`} className="text-cyan-300 hover:text-cyan-200 font-medium">
            Create an account
          </Link>
        </p>
      }
    />
  );
}

export default function LoginPage() {
  return (
    <AuthShell tagline="Your conversations are waiting in the Nexus.">
      <Suspense>
        <LoginForm />
      </Suspense>
    </AuthShell>
  );
}
