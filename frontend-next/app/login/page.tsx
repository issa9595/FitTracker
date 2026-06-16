"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Dumbbell,
  Users,
  BellRing,
  Mail,
  Lock,
  User,
  Eye,
  EyeOff,
  Loader2,
  TriangleAlert,
} from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

type Mode = "login" | "register";

const FEATURES = [
  { icon: Dumbbell, text: "Logue chaque séance et suis ta progression" },
  { icon: Users, text: "Suis tes amis et reste motivé ensemble" },
  { icon: BellRing, text: "Notifications en temps réel à chaque exploit" },
];

export default function LoginPage() {
  const { status, login, register } = useAuth();
  const router = useRouter();

  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("test@fittracker.dev");
  const [displayName, setDisplayName] = useState("Mon Nom");
  const [password, setPassword] = useState("ChangeMe123!");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (status === "authed") router.replace("/");
  }, [status, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (mode === "login") {
        await login(email.trim(), password);
      } else {
        await register(email.trim(), password, displayName.trim());
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erreur inconnue");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid min-h-dvh lg:grid-cols-2">
      {/* Brand panel (desktop) */}
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-brand p-12 text-brand-foreground lg:flex">
        <div
          aria-hidden
          className="pointer-events-none absolute -right-24 -top-24 size-80 rounded-full bg-white/10"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-32 -left-16 size-96 rounded-full bg-black/10"
        />
        <div className="relative flex items-center gap-3">
          <span className="grid size-11 place-items-center rounded-2xl bg-white/15 font-display text-xl font-bold">
            FT
          </span>
          <span className="font-display text-2xl font-bold tracking-tight">
            FitTracker
          </span>
        </div>
        <div className="relative">
          <h1 className="font-display text-5xl font-bold uppercase leading-[0.95] tracking-tight">
            Chaque séance
            <br />
            te rapproche
            <br />
            du sommet
          </h1>
          <ul className="mt-10 space-y-4">
            {FEATURES.map((f) => (
              <li key={f.text} className="flex items-center gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-white/15">
                  <f.icon className="size-5" aria-hidden />
                </span>
                <span className="text-[15px] text-white/90">{f.text}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-white/70">
          Suivi d&apos;entraînement &amp; notifications temps réel
        </p>
      </aside>

      {/* Form */}
      <main className="flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <span className="grid size-10 place-items-center rounded-xl bg-brand font-display text-lg font-bold text-brand-foreground">
              FT
            </span>
            <span className="font-display text-2xl font-bold tracking-tight">
              FitTracker
            </span>
          </div>

          <h2 className="font-display text-3xl font-bold tracking-tight">
            {mode === "login" ? "Content de te revoir" : "Crée ton compte"}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {mode === "login"
              ? "Connecte-toi pour reprendre l'entraînement."
              : "Rejoins FitTracker en quelques secondes."}
          </p>

          <Tabs
            value={mode}
            onValueChange={(v) => {
              setMode(v as Mode);
              setError("");
            }}
            className="mt-6"
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login" className="cursor-pointer">
                Connexion
              </TabsTrigger>
              <TabsTrigger value="register" className="cursor-pointer">
                Inscription
              </TabsTrigger>
            </TabsList>
          </Tabs>

          <form onSubmit={submit} className="mt-6 space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <div className="relative">
                <Mail
                  className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                  aria-hidden
                />
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="h-11 pl-9"
                  required
                />
              </div>
            </div>

            {mode === "register" && (
              <div className="space-y-1.5">
                <Label htmlFor="name">Nom affiché</Label>
                <div className="relative">
                  <User
                    className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                    aria-hidden
                  />
                  <Input
                    id="name"
                    type="text"
                    autoComplete="name"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="h-11 pl-9"
                  />
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="password">Mot de passe</Label>
              <div className="relative">
                <Lock
                  className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                  aria-hidden
                />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete={
                    mode === "login" ? "current-password" : "new-password"
                  }
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="h-11 px-9"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((s) => !s)}
                  aria-label={
                    showPassword
                      ? "Masquer le mot de passe"
                      : "Afficher le mot de passe"
                  }
                  className="absolute right-2 top-1/2 grid size-7 -translate-y-1/2 cursor-pointer place-items-center rounded-md text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {showPassword ? (
                    <EyeOff className="size-4" aria-hidden />
                  ) : (
                    <Eye className="size-4" aria-hidden />
                  )}
                </button>
              </div>
            </div>

            {error && (
              <p
                role="alert"
                className="flex items-start gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                <TriangleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
                {error}
              </p>
            )}

            <Button
              type="submit"
              disabled={busy}
              className="h-11 w-full cursor-pointer text-[15px] font-semibold"
            >
              {busy && <Loader2 className="size-4 animate-spin" aria-hidden />}
              {mode === "login" ? "Se connecter" : "Créer le compte"}
            </Button>
          </form>
        </div>
      </main>
    </div>
  );
}
