import {
  Footprints,
  Dumbbell,
  Swords,
  Shield,
  Activity,
  type LucideIcon,
} from "lucide-react";

export interface SessionMeta {
  label: string;
  Icon: LucideIcon;
  chip: string; // background + text classes for the icon chip
}

const MAP: Record<string, SessionMeta> = {
  RUNNING: { label: "Course", Icon: Footprints, chip: "bg-blue-100 text-blue-700" },
  STRENGTH: { label: "Renfo", Icon: Dumbbell, chip: "bg-rose-100 text-rose-700" },
  MMA: { label: "MMA", Icon: Swords, chip: "bg-amber-100 text-amber-700" },
  WRESTLING: { label: "Lutte", Icon: Shield, chip: "bg-violet-100 text-violet-700" },
  OTHER: { label: "Autre", Icon: Activity, chip: "bg-emerald-100 text-emerald-700" },
};

export function sessionMeta(type: string): SessionMeta {
  return MAP[type] ?? MAP.OTHER;
}

export const SESSION_TYPES = [
  "RUNNING",
  "STRENGTH",
  "MMA",
  "WRESTLING",
  "OTHER",
] as const;
