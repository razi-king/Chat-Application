"use client";
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { AuthService, RegisterPayload } from "@/services/AuthService";
import { ApiBase, AUTH_EXPIRED_EVENT, TOKEN_KEY, getToken } from "@/services/api";
import type { User } from "@/types";

interface AuthContextValue {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (identifier: string, password: string) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<User>;
  logout: () => Promise<void>;
  setUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function saveToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // Private Mode / Blocked Storage -> Session Only Lives In Memory
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Free Hosting Sleeps Idle Servers (~50 s Cold Start). Ping As Soon As Any Page Opens So The
  // Backend Is Already Waking Up While The User Reads The Landing Page / Types Their Password.
  useEffect(() => {
    fetch(`${ApiBase}/health`).catch(() => {});
  }, []);

  // Restore The Session On Page Load
  useEffect(() => {
    const stored = getToken();
    if (!stored) {
      // localStorage Only Exists In The Browser, So The Session Can Only Be Read After Mount
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLoading(false);
      return;
    }
    setToken(stored);
    AuthService.me()
      .then(setUser)
      .catch(() => {
        saveToken(null);
        setToken(null);
      })
      .finally(() => setLoading(false));
  }, []);

  // The Axios Interceptor Fires This When Any Call Returns 401
  useEffect(() => {
    const onExpired = () => {
      if (!getToken()) return;
      saveToken(null);
      setToken(null);
      setUser(null);
      toast.error("Your session expired. Please log in again.", { id: "session-expired" });
      const next = window.location.pathname;
      router.replace(`/login?next=${encodeURIComponent(next)}`);
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, [router]);

  const login = useCallback(async (identifier: string, password: string) => {
    const res = await AuthService.login(identifier, password);
    saveToken(res.token);
    setToken(res.token);
    setUser(res.user);
    return res.user;
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const res = await AuthService.register(payload);
    saveToken(res.token);
    setToken(res.token);
    setUser(res.user);
    return res.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await AuthService.logout();
    } catch {
      // Token Might Already Be Invalid; We Log Out Locally Anyway
    }
    saveToken(null);
    setToken(null);
    setUser(null);
    router.replace("/login");
  }, [router]);

  const value = useMemo(
    () => ({ user, token, loading, login, register, logout, setUser }),
    [user, token, loading, login, register, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
