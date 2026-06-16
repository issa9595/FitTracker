"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import {
  Copy,
  UserPlus,
  UserMinus,
  Users,
  Activity,
  Loader2,
} from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { useNotifications } from "@/components/NotificationsProvider";
import { notificationMeta } from "@/components/notification-meta";
import { api, apiList } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import type { Follow } from "@/lib/types";

export default function SocialPage() {
  const { me } = useAuth();
  const { notifications } = useNotifications();
  const [follows, setFollows] = useState<Follow[]>([]);
  const [followId, setFollowId] = useState("");
  const [busy, setBusy] = useState(false);

  const loadFollows = useCallback(async () => {
    if (!me) return;
    const list = await apiList<Follow>(`/api/v1/users/${me.id}/follows`);
    setFollows(list);
  }, [me]);

  useEffect(() => {
    loadFollows().catch(() => {});
  }, [loadFollows]);

  const follow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!me) return;
    const id = followId.trim();
    if (!id) return;
    setBusy(true);
    try {
      await api(`/api/v1/users/${me.id}/follows`, {
        method: "POST",
        body: JSON.stringify({ followeeId: id }),
      });
      setFollowId("");
      await loadFollows();
      toast.success("Abonnement ajouté");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Erreur");
    } finally {
      setBusy(false);
    }
  };

  const unfollow = async (followeeId: string) => {
    if (!me) return;
    try {
      await api(`/api/v1/users/${me.id}/follows/${followeeId}`, {
        method: "DELETE",
      });
      await loadFollows();
      toast.success("Abonnement retiré");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Erreur");
    }
  };

  const copyId = async () => {
    if (!me) return;
    try {
      await navigator.clipboard?.writeText(me.id);
      toast.success("Identifiant copié");
    } catch {
      toast.error("Copie impossible");
    }
  };

  const feed = notifications.slice(0, 8);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-bold tracking-tight sm:text-4xl">
          Social
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Suis d&apos;autres athlètes et reste motivé ensemble.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          {/* Follow card */}
          <section className="rounded-2xl border border-border bg-card p-5">
            <h2 className="font-display text-lg font-bold">Suivre un athlète</h2>

            <div className="mt-3">
              <Label className="text-xs text-muted-foreground">
                Ton identifiant (à partager)
              </Label>
              <div className="mt-1 flex items-center gap-2 rounded-xl bg-muted px-3 py-2">
                <code className="min-w-0 flex-1 truncate font-mono text-xs text-brand-strong">
                  {me?.id}
                </code>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  aria-label="Copier mon identifiant"
                  className="cursor-pointer hover:bg-accent hover:text-brand-strong"
                  onClick={copyId}
                >
                  <Copy className="size-4" aria-hidden />
                </Button>
              </div>
            </div>

            <form onSubmit={follow} className="mt-4 flex gap-2">
              <Input
                value={followId}
                onChange={(e) => setFollowId(e.target.value)}
                placeholder="Identifiant à suivre (UUID)"
                aria-label="Identifiant à suivre"
                className="h-11 flex-1 font-mono text-sm"
              />
              <Button
                type="submit"
                disabled={busy || !followId.trim()}
                className="h-11 cursor-pointer font-semibold"
              >
                {busy ? (
                  <Loader2 className="size-4 animate-spin" aria-hidden />
                ) : (
                  <UserPlus className="size-4" aria-hidden />
                )}
                Suivre
              </Button>
            </form>
          </section>

          {/* Following list */}
          <section className="rounded-2xl border border-border bg-card p-5">
            <h2 className="flex items-center gap-2 font-display text-lg font-bold">
              <Users className="size-5 text-brand" aria-hidden />
              Je suis
              <span className="ml-1 rounded-full bg-secondary px-2 py-0.5 text-sm font-semibold text-brand-strong tabular">
                {follows.length}
              </span>
            </h2>

            {follows.length === 0 ? (
              <p className="mt-4 text-sm text-muted-foreground">
                Tu ne suis personne pour l&apos;instant.
              </p>
            ) : (
              <ul className="mt-4 space-y-2">
                {follows.map((f) => (
                  <li
                    key={f.followeeId}
                    className="flex items-center gap-3 rounded-xl bg-secondary/40 px-3 py-2"
                  >
                    <Avatar className="size-9 border border-border">
                      <AvatarFallback className="bg-card font-display text-xs font-semibold text-brand-strong uppercase">
                        {f.followeeId.slice(0, 2)}
                      </AvatarFallback>
                    </Avatar>
                    <code className="min-w-0 flex-1 truncate font-mono text-xs text-muted-foreground">
                      {f.followeeId}
                    </code>
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          aria-label="Retirer l'abonnement"
                          className="cursor-pointer text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                        >
                          <UserMinus className="size-4" aria-hidden />
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>
                            Ne plus suivre cet athlète ?
                          </AlertDialogTitle>
                          <AlertDialogDescription>
                            Tu ne recevras plus ses notifications d&apos;activité.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel className="cursor-pointer">
                            Annuler
                          </AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => unfollow(f.followeeId)}
                            className="cursor-pointer bg-destructive text-white hover:bg-destructive/90"
                          >
                            Retirer
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        {/* Activity feed */}
        <section className="rounded-2xl border border-border bg-card p-5">
          <h2 className="flex items-center gap-2 font-display text-lg font-bold">
            <Activity className="size-5 text-brand" aria-hidden />
            Activité récente
          </h2>

          {feed.length === 0 ? (
            <p className="mt-4 text-sm text-muted-foreground">
              Aucune activité pour l&apos;instant. Suis des athlètes pour voir
              leurs exploits ici.
            </p>
          ) : (
            <ul className="mt-4 space-y-2">
              {feed.map((n) => {
                const meta = notificationMeta(n.type);
                const Icon = meta.Icon;
                return (
                  <li
                    key={n.id}
                    className="flex items-start gap-3 rounded-xl border border-border/70 px-3 py-2.5"
                  >
                    <span
                      className={cn(
                        "grid size-9 shrink-0 place-items-center rounded-xl",
                        meta.chip,
                      )}
                    >
                      <Icon className="size-4.5" aria-hidden />
                    </span>
                    <div className="min-w-0">
                      <p className="text-sm font-medium leading-tight">
                        {meta.label}
                      </p>
                      {n.createdAt && (
                        <p className="text-xs text-muted-foreground tabular">
                          {new Date(n.createdAt).toLocaleString("fr-FR")}
                        </p>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
          <p className="mt-4 text-xs text-muted-foreground">
            Le fil complet et les notifications non lues sont sur l&apos;écran
            Notifications.
          </p>
        </section>
      </div>
    </div>
  );
}
