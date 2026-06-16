"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { api, getToken, setToken } from "@/lib/api";
import type { AuthResponse, Me } from "@/lib/types";

type Status = "loading" | "authed" | "anon";

interface AuthContextValue {
  status: Status;
  me: Me | null;
  login: (email: string, password: string) => Promise<void>;
  register: (
    email: string,
    password: string,
    displayName: string,
  ) => Promise<void>;
  logout: () => void;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [status, setStatus] = useState<Status>("loading");
  const [me, setMe] = useState<Me | null>(null);

  const loadMe = useCallback(async () => {
    const profile = await api<Me>("/api/v1/users/me");
    setMe(profile);
    return profile;
  }, []);

  // Au montage : reprend la session si un token est present et valide.
  useEffect(() => {
    if (!getToken()) {
      setStatus("anon");
      return;
    }
    loadMe()
      .then(() => setStatus("authed"))
      .catch(() => {
        setToken(null);
        setStatus("anon");
      });
  }, [loadMe]);

  const login = useCallback(
    async (email: string, password: string) => {
      const auth = await api<AuthResponse>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(auth.accessToken);
      await loadMe();
      setStatus("authed");
      router.push("/");
    },
    [loadMe, router],
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string) => {
      const auth = await api<AuthResponse>("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify({
          email,
          password,
          displayName: displayName || email,
        }),
      });
      setToken(auth.accessToken);
      await loadMe();
      setStatus("authed");
      router.push("/");
    },
    [loadMe, router],
  );

  const logout = useCallback(() => {
    setToken(null);
    setMe(null);
    setStatus("anon");
    router.push("/login");
  }, [router]);

  const refreshMe = useCallback(async () => {
    await loadMe();
  }, [loadMe]);

  return (
    <AuthContext.Provider
      value={{ status, me, login, register, logout, refreshMe }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth doit etre utilise dans <AuthProvider>");
  return ctx;
}
