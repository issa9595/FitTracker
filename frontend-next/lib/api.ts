// Wrapper fetch pour l'API FitTracker.
// - injecte le JWT Bearer (stocke dans localStorage)
// - parse le corps JSON et remonte une erreur lisible (detail/title RFC-7807)
// - apiList() extrait `content` des reponses paginees Spring Data
//
// Porte la logique de `frontend/index.html` (version vanilla) en TypeScript.

export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TOKEN_KEY = "fittracker.token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (typeof window === "undefined") return;
  if (token) window.localStorage.setItem(TOKEN_KEY, token);
  else window.localStorage.removeItem(TOKEN_KEY);
}

interface ProblemDetail {
  detail?: string;
  title?: string;
}

export async function api<T = unknown>(
  path: string,
  opts: RequestInit = {},
): Promise<T> {
  const token = getToken();
  const res = await fetch(API_URL + path, {
    ...opts,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(opts.headers ?? {}),
    },
  });

  if (res.status === 204) return null as T;

  const body = (await res.json().catch(() => ({}))) as T & ProblemDetail;
  if (!res.ok) {
    const problem = body as ProblemDetail;
    throw new Error(problem.detail ?? problem.title ?? "Erreur " + res.status);
  }
  return body as T;
}

// Renvoie un tableau, que l'endpoint soit pagine (Page<T>.content) ou deja une liste.
export async function apiList<T = unknown>(
  path: string,
  opts: RequestInit = {},
): Promise<T[]> {
  const body = await api<{ content?: T[] } | T[]>(path, opts);
  if (Array.isArray(body)) return body;
  return body?.content ?? [];
}
