// Types alignes sur les DTO du back-end FitTracker (cf. frontend/index.html).

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
}

export interface Me {
  id: string;
  email: string;
  displayName?: string;
}

export interface Exercise {
  id: string;
  name: string;
  unit: string;
  category?: string;
}

export interface SessionExercise {
  id?: string;
  exerciseId: string;
  sets?: number | null;
  reps?: number | null;
  weightKg?: number | null;
  distanceM?: number | null;
  timeSeconds?: number | null;
}

export type TrainingType = "RUNNING" | "STRENGTH" | "MMA" | "WRESTLING" | "OTHER";

export interface TrainingSession {
  id: string;
  type: TrainingType | string;
  startedAt: string;
  durationSeconds?: number;
  notes?: string | null;
  exercises?: SessionExercise[];
}

export interface Follow {
  id?: string;
  followerId?: string;
  followeeId: string;
  createdAt?: string;
}

export type NotificationType =
  | "FRIEND_SESSION_COMPLETED"
  | "NEW_PR"
  | "PROGRAM_INVITE"
  | "ACHIEVEMENT"
  | string;

// Nomme AppNotification pour ne pas masquer le type global `Notification` du DOM.
export interface AppNotification {
  id: string;
  type: NotificationType;
  readAt?: string | null;
  createdAt?: string;
  payload?: unknown;
}

export interface UserProfile {
  heightCm?: number | null;
  weightKg?: number | null;
  goalWeightKg?: number | null;
  bio?: string | null;
}

// Forme paginee renvoyee par Spring Data (Page<T>).
export interface Page<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}
