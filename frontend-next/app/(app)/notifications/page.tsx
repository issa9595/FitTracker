"use client";

import { useEffect } from "react";
import { toast } from "sonner";
import { Check, CheckCheck, BellOff, Radio } from "lucide-react";
import { useNotifications } from "@/components/NotificationsProvider";
import { notificationMeta } from "@/components/notification-meta";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

export default function NotificationsPage() {
  const { notifications, unreadCount, connected, markRead, refresh, resetLiveCount } =
    useNotifications();

  useEffect(() => {
    resetLiveCount();
    refresh().catch(() => {});
  }, [resetLiveCount, refresh]);

  const markAll = async () => {
    const unread = notifications.filter((n) => !n.readAt);
    if (unread.length === 0) return;
    await Promise.allSettled(
      unread.map((n) =>
        api(`/api/v1/notifications/${n.id}/read`, { method: "PATCH" }),
      ),
    );
    await refresh();
    toast.success("Tout marqué comme lu");
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight sm:text-4xl">
            Notifications
          </h1>
          <p
            className={cn(
              "mt-1 inline-flex items-center gap-1.5 text-sm",
              connected ? "text-emerald-600" : "text-muted-foreground",
            )}
          >
            <Radio
              className={cn("size-4", connected && "animate-pulse")}
              aria-hidden
            />
            {connected ? "Temps réel actif" : "Hors ligne"}
          </p>
        </div>
        <Button
          variant="outline"
          onClick={markAll}
          disabled={unreadCount === 0}
          className="cursor-pointer"
        >
          <CheckCheck className="size-4" aria-hidden />
          Tout marquer lu
        </Button>
      </header>

      {notifications.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border bg-card/50 px-6 py-16 text-center">
          <span className="grid size-12 place-items-center rounded-2xl bg-muted text-muted-foreground">
            <BellOff className="size-6" aria-hidden />
          </span>
          <p className="text-sm text-muted-foreground">
            Aucune notification pour l&apos;instant.
          </p>
        </div>
      ) : (
        <ul aria-live="polite" className="space-y-2">
          {notifications.map((n) => {
            const meta = notificationMeta(n.type);
            const Icon = meta.Icon;
            const unread = !n.readAt;
            return (
              <li
                key={n.id}
                className={cn(
                  "flex items-start gap-3 rounded-2xl border p-4 transition-colors",
                  unread
                    ? "border-brand/30 bg-accent/50"
                    : "border-border bg-card",
                )}
              >
                <span
                  className={cn(
                    "grid size-10 shrink-0 place-items-center rounded-xl",
                    meta.chip,
                  )}
                >
                  <Icon className="size-5" aria-hidden />
                </span>
                <div className="min-w-0 flex-1">
                  <p
                    className={cn(
                      "leading-tight",
                      unread ? "font-semibold" : "font-medium text-foreground/80",
                    )}
                  >
                    {meta.label}
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground tabular">
                    {n.createdAt
                      ? new Date(n.createdAt).toLocaleString("fr-FR")
                      : "à l'instant"}
                  </p>
                </div>
                {unread && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => markRead(n.id)}
                    className="shrink-0 cursor-pointer text-brand-strong hover:bg-accent"
                  >
                    <Check className="size-4" aria-hidden />
                    <span className="hidden sm:inline">Marquer lu</span>
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
