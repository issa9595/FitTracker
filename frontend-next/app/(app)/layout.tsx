"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { NotificationsProvider } from "@/components/NotificationsProvider";
import TopBar from "@/components/TopBar";
import BottomNav from "@/components/BottomNav";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "anon") router.replace("/login");
  }, [status, router]);

  if (status !== "authed") {
    return (
      <div className="flex flex-1 items-center justify-center gap-2 text-muted-foreground">
        <Loader2 className="size-5 animate-spin" aria-hidden />
        <span className="text-sm">Chargement…</span>
      </div>
    );
  }

  return (
    <NotificationsProvider>
      <TopBar />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-28 pt-6 md:px-6 md:pb-12">
        {children}
      </main>
      <BottomNav />
    </NotificationsProvider>
  );
}
