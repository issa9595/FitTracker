"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { Client } from "@stomp/stompjs";
import { api, apiList, API_URL, getToken } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import type { AppNotification } from "@/lib/types";

interface NotificationsContextValue {
  notifications: AppNotification[];
  unreadCount: number;
  liveCount: number;
  connected: boolean;
  refresh: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  resetLiveCount: () => void;
}

const NotificationsContext = createContext<NotificationsContextValue | null>(
  null,
);

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { me } = useAuth();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [connected, setConnected] = useState(false);
  const [liveCount, setLiveCount] = useState(0);

  const refresh = useCallback(async () => {
    const items = await apiList<AppNotification>(
      "/api/v1/notifications?size=30",
    );
    setNotifications(items);
  }, []);

  const markRead = useCallback(
    async (id: string) => {
      await api(`/api/v1/notifications/${id}/read`, { method: "PATCH" });
      await refresh();
    },
    [refresh],
  );

  const resetLiveCount = useCallback(() => setLiveCount(0), []);

  // Connexion STOMP/SockJS + abonnement au topic personnel, le temps de la session.
  useEffect(() => {
    if (!me) return;

    refresh().catch(() => {});

    const token = getToken();
    let active = true;
    let client: Client | undefined;

    (async () => {
      // Gotcha sockjs-client : il s'attend a trouver un objet global `global`.
      const w = window as unknown as { global?: unknown };
      w.global ||= window;

      const SockJS = (await import("sockjs-client")).default;
      const { Client: StompClient } = await import("@stomp/stompjs");
      if (!active) return;

      client = new StompClient({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          setConnected(true);
          client?.subscribe(`/topic/notifications/${me.id}`, (message) => {
            try {
              const incoming = JSON.parse(message.body) as AppNotification;
              setNotifications((prev) => [
                incoming,
                ...prev.filter((n) => n.id !== incoming.id),
              ]);
              setLiveCount((count) => count + 1);
              // Resynchronise avec le serveur (readAt, ordre, etc.).
              refresh().catch(() => {});
            } catch {
              /* payload non-JSON : ignore */
            }
          });
        },
        onWebSocketClose: () => setConnected(false),
        onStompError: () => setConnected(false),
      });

      client.activate();
    })();

    return () => {
      active = false;
      setConnected(false);
      client?.deactivate();
    };
  }, [me, refresh]);

  const unreadCount = notifications.filter((n) => !n.readAt).length;

  return (
    <NotificationsContext.Provider
      value={{
        notifications,
        unreadCount,
        liveCount,
        connected,
        refresh,
        markRead,
        resetLiveCount,
      }}
    >
      {children}
    </NotificationsContext.Provider>
  );
}

export function useNotifications(): NotificationsContextValue {
  const ctx = useContext(NotificationsContext);
  if (!ctx)
    throw new Error(
      "useNotifications doit etre utilise dans <NotificationsProvider>",
    );
  return ctx;
}
