"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Plus, Trash2, Clock, CalendarDays, Loader2, Dumbbell } from "lucide-react";
import { api, apiList } from "@/lib/api";
import { cn } from "@/lib/utils";
import { sessionMeta, SESSION_TYPES } from "@/components/session-meta";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import type {
  Exercise,
  SessionExercise,
  TrainingSession,
  TrainingType,
} from "@/lib/types";

function exerciseDetails(e: SessionExercise): string {
  return [
    e.sets ? `${e.sets}×${e.reps ?? ""}` : null,
    e.weightKg ? `${e.weightKg} kg` : null,
    e.distanceM ? `${e.distanceM} m` : null,
    e.timeSeconds ? `${e.timeSeconds} s` : null,
  ]
    .filter(Boolean)
    .join(" · ");
}

function SessionCard({
  session,
  exercises,
  onChanged,
}: {
  session: TrainingSession;
  exercises: Exercise[];
  onChanged: () => void;
}) {
  const [exerciseId, setExerciseId] = useState(exercises[0]?.id ?? "");
  const [reps, setReps] = useState("");
  const [kg, setKg] = useState("");
  const [busy, setBusy] = useState(false);
  const meta = sessionMeta(session.type);
  const Icon = meta.Icon;

  const addExo = async () => {
    if (!exerciseId) return;
    setBusy(true);
    try {
      await api(`/api/v1/training-sessions/${session.id}/exercises`, {
        method: "POST",
        body: JSON.stringify({
          exerciseId,
          sets: 1,
          reps: parseInt(reps || "0", 10) || null,
          weightKg: parseFloat(kg) || null,
        }),
      });
      setReps("");
      setKg("");
      onChanged();
      toast.success("Exercice ajouté");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Erreur");
    } finally {
      setBusy(false);
    }
  };

  const del = async () => {
    try {
      await api(`/api/v1/training-sessions/${session.id}`, { method: "DELETE" });
      onChanged();
      toast.success("Séance supprimée");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Erreur");
    }
  };

  const sessionExercises = session.exercises ?? [];

  return (
    <article className="rounded-2xl border border-border bg-card p-4 sm:p-5">
      <header className="flex items-start gap-3">
        <span className={cn("grid size-11 shrink-0 place-items-center rounded-xl", meta.chip)}>
          <Icon className="size-5.5" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="font-display text-xl font-bold leading-tight">
            {meta.label}
          </h3>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <Clock className="size-3.5" aria-hidden />
              <span className="tabular">
                {Math.round((session.durationSeconds ?? 0) / 60)} min
              </span>
            </span>
            <span className="inline-flex items-center gap-1">
              <CalendarDays className="size-3.5" aria-hidden />
              <span className="tabular">
                {new Date(session.startedAt).toLocaleDateString("fr-FR")}
              </span>
            </span>
          </div>
        </div>

        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Supprimer la séance"
              className="cursor-pointer text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            >
              <Trash2 className="size-4" aria-hidden />
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer cette séance ?</AlertDialogTitle>
              <AlertDialogDescription>
                Cette action est irréversible. La séance {meta.label} et ses
                exercices seront définitivement supprimés.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className="cursor-pointer">
                Annuler
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={del}
                className="cursor-pointer bg-destructive text-white hover:bg-destructive/90"
              >
                Supprimer
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </header>

      {session.notes && (
        <p className="mt-3 rounded-lg bg-muted px-3 py-2 text-sm text-foreground/80">
          {session.notes}
        </p>
      )}

      <ul className="mt-3 space-y-1.5">
        {sessionExercises.length === 0 ? (
          <li className="text-sm text-muted-foreground">Aucun exercice ajouté.</li>
        ) : (
          sessionExercises.map((e, i) => {
            const ex = exercises.find((x) => x.id === e.exerciseId);
            const details = exerciseDetails(e);
            return (
              <li
                key={e.id ?? `${e.exerciseId}-${i}`}
                className="flex items-center justify-between gap-2 rounded-lg bg-secondary/50 px-3 py-1.5 text-sm"
              >
                <span className="font-medium text-brand-strong">
                  {ex?.name ?? e.exerciseId}
                </span>
                {details && (
                  <span className="text-muted-foreground tabular">{details}</span>
                )}
              </li>
            );
          })
        )}
      </ul>

      <div className="mt-4 flex flex-col gap-2 border-t border-border pt-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <Label className="sr-only">Exercice</Label>
          <Select value={exerciseId} onValueChange={setExerciseId}>
            <SelectTrigger className="w-full cursor-pointer">
              <SelectValue placeholder="Choisir un exercice" />
            </SelectTrigger>
            <SelectContent>
              {exercises.map((x) => (
                <SelectItem key={x.id} value={x.id} className="cursor-pointer">
                  {x.name}
                  <span className="text-muted-foreground"> · {x.unit}</span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Input
          type="number"
          inputMode="numeric"
          placeholder="reps"
          aria-label="Répétitions"
          value={reps}
          onChange={(e) => setReps(e.target.value)}
          className="w-full sm:w-20"
        />
        <Input
          type="number"
          inputMode="decimal"
          step="0.5"
          placeholder="kg"
          aria-label="Poids en kg"
          value={kg}
          onChange={(e) => setKg(e.target.value)}
          className="w-full sm:w-20"
        />
        <Button
          variant="secondary"
          onClick={addExo}
          disabled={busy || !exerciseId}
          className="cursor-pointer"
        >
          {busy ? (
            <Loader2 className="size-4 animate-spin" aria-hidden />
          ) : (
            <Plus className="size-4" aria-hidden />
          )}
          Exo
        </Button>
      </div>
    </article>
  );
}

export default function SessionsPage() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [type, setType] = useState<TrainingType>("RUNNING");
  const [minutes, setMinutes] = useState("30");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);

  const loadSessions = useCallback(async () => {
    const items = await apiList<TrainingSession>(
      "/api/v1/training-sessions?size=50&sort=startedAt,desc",
    );
    setSessions(items);
  }, []);

  useEffect(() => {
    apiList<Exercise>("/api/v1/exercises?size=50")
      .then(setExercises)
      .catch(() => {});
    loadSessions()
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [loadSessions]);

  const createSession = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api("/api/v1/training-sessions", {
        method: "POST",
        body: JSON.stringify({
          startedAt: new Date().toISOString(),
          durationSeconds: (parseInt(minutes || "0", 10) || 0) * 60,
          type,
          notes: notes || null,
        }),
      });
      setNotes("");
      await loadSessions();
      toast.success("Séance créée");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Erreur");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-bold tracking-tight sm:text-4xl">
          Séances
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Crée tes séances et ajoute tes exercices depuis le catalogue.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Create form */}
        <form
          onSubmit={createSession}
          className="h-fit space-y-4 rounded-2xl border border-border bg-card p-5 lg:sticky lg:top-20 lg:col-span-1"
        >
          <h2 className="font-display text-lg font-bold">Nouvelle séance</h2>

          <div className="space-y-1.5">
            <Label htmlFor="s-type">Type</Label>
            <Select
              value={type}
              onValueChange={(v) => setType(v as TrainingType)}
            >
              <SelectTrigger id="s-type" className="w-full cursor-pointer">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SESSION_TYPES.map((t) => (
                  <SelectItem key={t} value={t} className="cursor-pointer">
                    {sessionMeta(t).label}
                    <span className="text-muted-foreground"> · {t}</span>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="s-min">Durée (minutes)</Label>
            <Input
              id="s-min"
              type="number"
              inputMode="numeric"
              min="0"
              value={minutes}
              onChange={(e) => setMinutes(e.target.value)}
              className="h-11"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="s-notes">Notes</Label>
            <Input
              id="s-notes"
              type="text"
              placeholder="Optionnel"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="h-11"
            />
          </div>

          <Button
            type="submit"
            disabled={busy}
            className="h-11 w-full cursor-pointer font-semibold"
          >
            {busy ? (
              <Loader2 className="size-4 animate-spin" aria-hidden />
            ) : (
              <Plus className="size-4" aria-hidden />
            )}
            Créer la séance
          </Button>
        </form>

        {/* List */}
        <div className="space-y-3 lg:col-span-2">
          <h2 className="font-display text-lg font-bold">Mes séances</h2>
          {loading ? (
            <div className="space-y-3">
              {[0, 1].map((i) => (
                <Skeleton key={i} className="h-40 w-full rounded-2xl" />
              ))}
            </div>
          ) : sessions.length === 0 ? (
            <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border bg-card/50 px-6 py-12 text-center">
              <span className="grid size-12 place-items-center rounded-2xl bg-rose-100 text-rose-700">
                <Dumbbell className="size-6" aria-hidden />
              </span>
              <p className="text-sm text-muted-foreground">
                Aucune séance — crée la première avec le formulaire.
              </p>
            </div>
          ) : (
            sessions.map((s) => (
              <SessionCard
                key={s.id}
                session={s}
                exercises={exercises}
                onChanged={loadSessions}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}
