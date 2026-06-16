"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Ruler, Weight, Target, Save, Loader2 } from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import type { UserProfile } from "@/lib/types";

function initials(name?: string, email?: string) {
  const base = (name || email || "?").trim();
  const parts = base.split(/[\s@.]+/).filter(Boolean);
  return (parts[0]?.[0] ?? "?").concat(parts[1]?.[0] ?? "").toUpperCase();
}

export default function ProfilePage() {
  const { me } = useAuth();
  const [heightCm, setHeightCm] = useState("");
  const [weightKg, setWeightKg] = useState("");
  const [goalWeightKg, setGoalWeightKg] = useState("");
  const [bio, setBio] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<UserProfile>("/api/v1/users/me/profile")
      .then((p) => {
        setHeightCm(p.heightCm?.toString() ?? "");
        setWeightKg(p.weightKg?.toString() ?? "");
        setGoalWeightKg(p.goalWeightKg?.toString() ?? "");
        setBio(p.bio ?? "");
      })
      .catch(() => {
        /* profil pas encore créé */
      });
  }, []);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api("/api/v1/users/me/profile", {
        method: "PUT",
        body: JSON.stringify({
          heightCm: parseInt(heightCm, 10) || null,
          weightKg: parseFloat(weightKg) || null,
          goalWeightKg: parseFloat(goalWeightKg) || null,
          bio: bio || null,
        }),
      });
      toast.success("Profil enregistré");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Erreur");
    } finally {
      setBusy(false);
    }
  };

  const fields = [
    {
      id: "height",
      label: "Taille",
      unit: "cm",
      icon: Ruler,
      value: heightCm,
      set: setHeightCm,
      step: "1",
    },
    {
      id: "weight",
      label: "Poids",
      unit: "kg",
      icon: Weight,
      value: weightKg,
      set: setWeightKg,
      step: "0.1",
    },
    {
      id: "goal",
      label: "Objectif",
      unit: "kg",
      icon: Target,
      value: goalWeightKg,
      set: setGoalWeightKg,
      step: "0.1",
    },
  ];

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <header>
        <h1 className="font-display text-3xl font-bold tracking-tight sm:text-4xl">
          Profil
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Tes mensurations et ton objectif.
        </p>
      </header>

      <div className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5">
        <Avatar className="size-14 border border-border">
          <AvatarFallback className="bg-secondary font-display text-xl font-bold text-brand-strong">
            {initials(me?.displayName, me?.email)}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <p className="truncate font-display text-xl font-bold">
            {me?.displayName || "Athlète"}
          </p>
          <p className="truncate text-sm text-muted-foreground">{me?.email}</p>
        </div>
      </div>

      <form
        onSubmit={save}
        className="space-y-5 rounded-2xl border border-border bg-card p-5"
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {fields.map((f) => {
            const Icon = f.icon;
            return (
              <div key={f.id} className="space-y-1.5">
                <Label htmlFor={f.id} className="flex items-center gap-1.5">
                  <Icon className="size-4 text-brand" aria-hidden />
                  {f.label}
                </Label>
                <div className="relative">
                  <Input
                    id={f.id}
                    type="number"
                    inputMode="decimal"
                    step={f.step}
                    value={f.value}
                    onChange={(e) => f.set(e.target.value)}
                    className="h-11 pr-10"
                  />
                  <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                    {f.unit}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="bio">Bio</Label>
          <Textarea
            id="bio"
            rows={3}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            placeholder="Parle un peu de tes objectifs sportifs…"
          />
        </div>

        <Button
          type="submit"
          disabled={busy}
          className="h-11 cursor-pointer font-semibold"
        >
          {busy ? (
            <Loader2 className="size-4 animate-spin" aria-hidden />
          ) : (
            <Save className="size-4" aria-hidden />
          )}
          Enregistrer
        </Button>
      </form>
    </div>
  );
}
