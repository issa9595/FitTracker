import {
  Dumbbell,
  TrendingUp,
  CalendarPlus,
  Trophy,
  Bell,
  type LucideIcon,
} from "lucide-react";

export interface NotificationMeta {
  label: string;
  Icon: LucideIcon;
  chip: string;
}

const MAP: Record<string, NotificationMeta> = {
  FRIEND_SESSION_COMPLETED: {
    label: "Un ami a terminé une séance",
    Icon: Dumbbell,
    chip: "bg-rose-100 text-rose-700",
  },
  NEW_PR: {
    label: "Nouveau record personnel",
    Icon: TrendingUp,
    chip: "bg-blue-100 text-blue-700",
  },
  PROGRAM_INVITE: {
    label: "Invitation à un programme",
    Icon: CalendarPlus,
    chip: "bg-amber-100 text-amber-700",
  },
  ACHIEVEMENT: {
    label: "Succès débloqué",
    Icon: Trophy,
    chip: "bg-emerald-100 text-emerald-700",
  },
};

export function notificationMeta(type: string): NotificationMeta {
  return MAP[type] ?? { label: type, Icon: Bell, chip: "bg-muted text-muted-foreground" };
}
