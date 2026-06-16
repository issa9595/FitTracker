"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  Dumbbell,
  Users,
  Bell,
  ListChecks,
  ArrowRight,
  ChevronRight,
  Clock,
} from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { useNotifications } from "@/components/NotificationsProvider";
import { CountUp } from "@/components/CountUp";
import { sessionMeta } from "@/components/session-meta";
import { apiList } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import type { Exercise, Follow, TrainingSession } from "@/lib/types";

interface StatDef {
  label: string;
  value: number;
  icon: typeof Dumbbell;
  chip: string;
  href?: string;
}

function StatCard({ stat, loading }: { stat: StatDef; loading: boolean }) {
  const Icon = stat.icon;
  const body = (
    <div
      className={cn(
        "group flex h-full flex-col gap-3 rounded-2xl border border-border bg-card p-4 transition-all sm:p-5",
        stat.href &&
          "cursor-pointer hover:-translate-y-0.5 hover:border-brand/40 hover:shadow-[0_10px_30px_-12px_rgba(225,29,72,0.35)]",
      )}
    >
      <div className="flex items-center justify-between">
        <span
          className={cn("grid size-10 place-items-center rounded-xl", stat.chip)}
        >
          <Icon className="size-5" aria-hidden />
        </span>
        {stat.href && (
          <ChevronRight
            className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5"
            aria-hidden
          />
        )}
      </div>
      <div>
        {loading ? (
          <Skeleton className="h-9 w-12" />
        ) : (
          <div className="font-display text-4xl font-bold leading-none tabular">
            <CountUp value={stat.value} />
          </div>
        )}
        <div className="mt-1 text-sm text-muted-foreground">{stat.label}</div>
      </div>
    </div>
  );

  return stat.href ? (
    <Link href={stat.href} className="rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
      {body}
    </Link>
  ) : (
    body
  );
}

export default function DashboardPage() {
  const { me } = useAuth();
  const { unreadCount } = useNotifications();
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [followsCount, setFollowsCount] = useState(0);
  const [exosCount, setExosCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!me) return;
    let alive = true;
    Promise.allSettled([
      apiList<TrainingSession>(
        "/api/v1/training-sessions?size=50&sort=startedAt,desc",
      ),
      apiList<Follow>(`/api/v1/users/${me.id}/follows`),
      apiList<Exercise>("/api/v1/exercises?size=50"),
    ]).then((res) => {
      if (!alive) return;
      if (res[0].status === "fulfilled") setSessions(res[0].value);
      if (res[1].status === "fulfilled") setFollowsCount(res[1].value.length);
      if (res[2].status === "fulfilled") setExosCount(res[2].value.length);
      setLoading(false);
    });
    return () => {
      alive = false;
    };
  }, [me]);

  const today = new Date().toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  const firstName = (me?.displayName || me?.email || "").split(/[\s@]+/)[0];

  const stats: StatDef[] = [
    { label: "Séances", value: sessions.length, icon: Dumbbell, chip: "bg-rose-100 text-rose-700", href: "/sessions" },
    { label: "Abonnements", value: followsCount, icon: Users, chip: "bg-blue-100 text-blue-700", href: "/social" },
    { label: "Notifs non lues", value: unreadCount, icon: Bell, chip: "bg-amber-100 text-amber-700", href: "/notifications" },
    { label: "Exercices au catalogue", value: exosCount, icon: ListChecks, chip: "bg-emerald-100 text-emerald-700" },
  ];

  const recent = sessions.slice(0, 5);

  return (
    <div className="space-y-8">
      <header>
        <p className="text-sm font-medium capitalize text-muted-foreground">
          {today}
        </p>
        <h1 className="mt-1 font-display text-3xl font-bold tracking-tight sm:text-4xl">
          Salut, {firstName || "athlète"}
        </h1>
      </header>

      <section
        aria-label="Statistiques"
        className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4"
      >
        {stats.map((s) => (
          <StatCard key={s.label} stat={s} loading={loading} />
        ))}
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-display text-xl font-bold tracking-tight">
            Dernières séances
          </h2>
          <Link
            href="/sessions"
            className="inline-flex items-center gap-1 rounded-full px-2 py-1 text-sm font-medium text-brand-strong transition-colors hover:bg-accent"
          >
            Voir tout
            <ArrowRight className="size-4" aria-hidden />
          </Link>
        </div>

        {loading ? (
          <div className="space-y-2">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-16 w-full rounded-2xl" />
            ))}
          </div>
        ) : recent.length === 0 ? (
          <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border bg-card/50 px-6 py-12 text-center">
            <span className="grid size-12 place-items-center rounded-2xl bg-rose-100 text-rose-700">
              <Dumbbell className="size-6" aria-hidden />
            </span>
            <div>
              <p className="font-medium">Aucune séance pour l&apos;instant</p>
              <p className="text-sm text-muted-foreground">
                Lance ta première séance pour démarrer ta progression.
              </p>
            </div>
            <Link
              href="/sessions"
              className="mt-1 inline-flex h-10 cursor-pointer items-center gap-2 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Créer une séance
              <ArrowRight className="size-4" aria-hidden />
            </Link>
          </div>
        ) : (
          <ul className="space-y-2">
            {recent.map((s) => {
              const meta = sessionMeta(s.type);
              const Icon = meta.Icon;
              return (
                <li key={s.id}>
                  <div className="flex items-center gap-3 rounded-2xl border border-border bg-card p-3 sm:p-4">
                    <span
                      className={cn(
                        "grid size-10 shrink-0 place-items-center rounded-xl",
                        meta.chip,
                      )}
                    >
                      <Icon className="size-5" aria-hidden />
                    </span>
                    <div className="min-w-0">
                      <p className="font-display text-lg font-semibold leading-tight">
                        {meta.label}
                      </p>
                      <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                        <Clock className="size-3.5" aria-hidden />
                        <span className="tabular">
                          {Math.round((s.durationSeconds ?? 0) / 60)} min
                        </span>
                      </p>
                    </div>
                    <time className="ml-auto shrink-0 text-sm text-muted-foreground tabular">
                      {new Date(s.startedAt).toLocaleDateString("fr-FR", {
                        day: "2-digit",
                        month: "short",
                      })}
                    </time>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}
