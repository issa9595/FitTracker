"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Bell, LogOut } from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { useNotifications } from "@/components/NotificationsProvider";
import { NAV_ITEMS } from "@/components/nav-items";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";

function initials(name?: string, email?: string) {
  const base = (name || email || "?").trim();
  const parts = base.split(/[\s@.]+/).filter(Boolean);
  return (parts[0]?.[0] ?? "?").concat(parts[1]?.[0] ?? "").toUpperCase();
}

export default function TopBar() {
  const pathname = usePathname();
  const router = useRouter();
  const { me, logout } = useAuth();
  const { connected, liveCount, resetLiveCount } = useNotifications();

  return (
    <header className="sticky top-0 z-40 border-b border-border/80 bg-background/85 backdrop-blur-md">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-3 px-4">
        <Link href="/" className="flex items-center gap-2.5">
          <span className="grid size-9 place-items-center rounded-xl bg-brand font-display text-lg font-bold text-brand-foreground shadow-sm">
            FT
          </span>
          <span className="font-display text-xl font-bold tracking-tight">
            FitTracker
          </span>
        </Link>

        <nav className="ml-4 hidden items-center gap-1 md:flex">
          {NAV_ITEMS.map((item) => {
            const active = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "inline-flex items-center gap-2 rounded-full px-3.5 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-accent text-brand-strong"
                    : "text-muted-foreground hover:bg-accent/60 hover:text-brand-strong",
                )}
              >
                <Icon className="size-4" aria-hidden />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2.5">
          <span
            className="flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground"
            title={connected ? "Temps réel connecté" : "Temps réel déconnecté"}
          >
            <span
              className={cn(
                "size-2 rounded-full",
                connected ? "bg-emerald-500" : "bg-destructive",
              )}
            />
            <span className="hidden sm:inline">
              {connected ? "Live" : "Hors ligne"}
            </span>
          </span>

          <Button
            variant="ghost"
            size="icon"
            aria-label={`Notifications${liveCount > 0 ? ` (${liveCount} nouvelle${liveCount > 1 ? "s" : ""})` : ""}`}
            className="relative size-11 cursor-pointer rounded-full text-foreground hover:bg-accent hover:text-brand-strong sm:size-10"
            onClick={() => {
              resetLiveCount();
              router.push("/notifications");
            }}
          >
            <Bell className="size-5" aria-hidden />
            {liveCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid min-w-5 place-items-center rounded-full bg-brand px-1 text-[11px] font-semibold leading-5 text-brand-foreground tabular">
                {liveCount > 9 ? "9+" : liveCount}
              </span>
            )}
          </Button>

          <div className="hidden items-center gap-2 sm:flex">
            <Avatar className="size-9 border border-border">
              <AvatarFallback className="bg-secondary font-display text-sm font-semibold text-brand-strong">
                {initials(me?.displayName, me?.email)}
              </AvatarFallback>
            </Avatar>
            <span className="max-w-[10rem] truncate text-sm font-medium">
              {me?.displayName || me?.email}
            </span>
          </div>

          <Button
            variant="ghost"
            size="icon"
            aria-label="Déconnexion"
            className="size-11 cursor-pointer rounded-full text-muted-foreground hover:bg-destructive/10 hover:text-destructive sm:size-10"
            onClick={logout}
          >
            <LogOut className="size-5" aria-hidden />
          </Button>
        </div>
      </div>
    </header>
  );
}
