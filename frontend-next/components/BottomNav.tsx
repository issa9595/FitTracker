"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useNotifications } from "@/components/NotificationsProvider";
import { NAV_ITEMS } from "@/components/nav-items";
import { cn } from "@/lib/utils";

export default function BottomNav() {
  const pathname = usePathname();
  const { liveCount } = useNotifications();

  return (
    <nav
      aria-label="Navigation principale"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-md md:hidden"
    >
      <ul className="mx-auto grid max-w-md grid-cols-5">
        {NAV_ITEMS.map((item) => {
          const active = pathname === item.href;
          const Icon = item.icon;
          const showBadge = item.href === "/notifications" && liveCount > 0;
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "relative flex min-h-[56px] flex-col items-center justify-center gap-1 px-1 py-2 text-[11px] font-medium transition-colors",
                  active ? "text-brand" : "text-muted-foreground",
                )}
              >
                <Icon className="size-5" aria-hidden />
                {item.short}
                {showBadge && (
                  <span className="absolute right-1/2 top-1.5 translate-x-3 rounded-full bg-brand px-1 text-[10px] font-semibold leading-4 text-brand-foreground tabular">
                    {liveCount > 9 ? "9+" : liveCount}
                  </span>
                )}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
