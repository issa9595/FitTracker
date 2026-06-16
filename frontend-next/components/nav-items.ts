import { LayoutDashboard, Dumbbell, Users, Bell, User } from "lucide-react";

export const NAV_ITEMS = [
  { href: "/", label: "Tableau de bord", short: "Accueil", icon: LayoutDashboard },
  { href: "/sessions", label: "Séances", short: "Séances", icon: Dumbbell },
  { href: "/social", label: "Social", short: "Social", icon: Users },
  { href: "/notifications", label: "Notifications", short: "Alertes", icon: Bell },
  { href: "/profile", label: "Profil", short: "Profil", icon: User },
] as const;
