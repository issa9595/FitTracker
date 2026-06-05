# API examples — FitTracker v1

> Phase 3. Tous les exemples partent du principe que l'utilisateur courant est
> `00000000-0000-0000-0000-000000000001` (stub Phase 3). Voir `common/security/CurrentUserProvider.java`.
> En Phase 6, le `Authorization: Bearer <jwt>` remplacera ce stub.

Sommaire :

1. [Inscription (POST /api/v1/auth/register)](#1-inscription)
2. [Login (POST /api/v1/auth/login)](#2-login)
3. [Récupérer l'utilisateur courant (GET /api/v1/users/me)](#3-utilisateur-courant)
4. [Mettre à jour le profil (PUT /api/v1/users/me/profile)](#4-profil)
5. [Référentiel exercices avec filtre (GET /api/v1/exercises)](#5-exercices-filtre)
6. [Créer une session (POST /api/v1/training-sessions)](#6-session-creation)
7. [Ajouter un exercice à une session (POST /api/v1/training-sessions/{id}/exercises)](#7-session-add-exercise)
8. [Lister les sessions avec filtrage AND (GET /api/v1/training-sessions?filter=...)](#8-sessions-filtre)
9. [Erreur de validation 400 RFC 7807 (POST /api/v1/programs)](#9-erreur-validation)
10. [Erreur métier 422 RFC 7807 (POST /api/v1/programs)](#10-erreur-metier)
11. [Notifications par curseur (GET /api/v1/notifications)](#11-notifications-cursor)
12. [Versioning par Accept (GET /api/v1/version-demo/widgets)](#12-versioning)

---

## Pagination — quand utiliser offset vs cursor

| Stratégie | Quand l'utiliser | Performance | Compatibilité |
|---|---|---|---|
| **Offset/limit** (`?page=N&size=M`) | Données stables, navigation type "page X / Y", besoin de `totalElements` | Coûteux passé N grand (OFFSET en SQL) | Familier pour les clients web |
| **Cursor** (`?cursor=…&size=M`) | Flux à fort volume, données ajoutées en continu (notifications, feed) | Constant, basé sur une clé indexée | Pas de compteur total, navigation only-next |

FitTracker utilise offset par défaut sur `/training-sessions`, `/programs`, `/exercises`, et cursor sur `/notifications` (et plus tard `/feed`).

---

## 1. Inscription <a id="1-inscription"></a>

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "jane@example.com",
    "password": "ChangeMe123!",
    "displayName": "Jane Doe"
  }'
```

→ `201 Created`

```json
{
  "userId": "8a3f29c1-...",
  "email": "jane@example.com",
  "accessToken": "stub-token-phase-6",
  "tokenType": "bearer",
  "expiresInSeconds": 900
}
```

---

## 2. Login <a id="2-login"></a>

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"ChangeMe123!"}'
```

→ `200 OK` — même corps qu'au register.

---

## 3. Utilisateur courant <a id="3-utilisateur-courant"></a>

```bash
curl http://localhost:8080/api/v1/users/me
```

→ `200 OK`

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "test@fittracker.dev",
  "displayName": "Test User",
  "createdAt": "2026-06-04T10:00:00Z",
  "_links": {
    "self":    { "href": "http://localhost:8080/api/v1/users/me" },
    "profile": { "href": "http://localhost:8080/api/v1/users/me/profile" }
  }
}
```

---

## 4. Profil <a id="4-profil"></a>

```bash
curl -X PUT http://localhost:8080/api/v1/users/me/profile \
  -H 'Content-Type: application/json' \
  -d '{"heightCm":180,"weightKg":75.0,"goalWeightKg":72.0,"bio":"Athlete amateur"}'
```

→ `200 OK`

```json
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "heightCm": 180,
  "weightKg": 75.0,
  "goalWeightKg": 72.0,
  "bio": "Athlete amateur",
  "_links": {
    "self": { "href": "http://localhost:8080/api/v1/users/me/profile" },
    "user": { "href": "http://localhost:8080/api/v1/users/me" }
  }
}
```

---

## 5. Référentiel exercices avec filtre <a id="5-exercices-filtre"></a>

```bash
curl 'http://localhost:8080/api/v1/exercises?filter=category:eq:STRENGTH,muscleGroup:like:dos&filterOp=AND&page=0&size=10&sort=name,asc'
```

→ `200 OK`

```json
{
  "content": [
    { "id": "...b3", "name": "Souleve de terre", "category": "STRENGTH", "muscleGroup": "dos", "unit": "REPS",
      "_links": { "self": {"href":"..."}, "collection": {"href":"..."} } },
    { "id": "...b4", "name": "Tractions",        "category": "STRENGTH", "muscleGroup": "dos", "unit": "REPS",
      "_links": { "self": {"href":"..."}, "collection": {"href":"..."} } }
  ],
  "page": 0, "size": 10, "totalElements": 2, "totalPages": 1,
  "_links": {
    "self":  { "href": "...?page=0&size=10&sort=name,asc&filter=...&filterOp=AND" },
    "first": { "href": "...?page=0&size=10&..." },
    "last":  { "href": "...?page=0&size=10&..." }
  }
}
```

**Opérateurs supportés** : `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `in` (valeurs séparées par `|`), `like`. **Combinator** : `AND` (défaut) ou `OR`.

---

## 6. Création de session <a id="6-session-creation"></a>

```bash
curl -X POST http://localhost:8080/api/v1/training-sessions \
  -H 'Content-Type: application/json' \
  -d '{
    "startedAt": "2026-06-04T18:00:00Z",
    "durationSeconds": 3600,
    "type": "STRENGTH",
    "notes": "Push day"
  }'
```

→ `201 Created` avec `Location: /api/v1/training-sessions/<uuid>`.

```json
{
  "id": "1b2e...",
  "userId": "00000000-...0001",
  "startedAt": "2026-06-04T18:00:00Z",
  "durationSeconds": 3600,
  "type": "STRENGTH",
  "notes": "Push day",
  "createdAt": "2026-06-04T18:00:01Z",
  "exercises": [],
  "_links": {
    "self":          { "href": ".../api/v1/training-sessions/1b2e..." },
    "collection":    { "href": ".../api/v1/training-sessions" },
    "add-exercise":  { "href": ".../api/v1/training-sessions/1b2e.../exercises" }
  }
}
```

---

## 7. Ajout d'exercice à une session <a id="7-session-add-exercise"></a>

```bash
curl -X POST http://localhost:8080/api/v1/training-sessions/1b2e.../exercises \
  -H 'Content-Type: application/json' \
  -d '{
    "exerciseId": "00000000-0000-0000-0000-0000000000b1",
    "position": 0,
    "sets": 4,
    "reps": 10,
    "weightKg": 80.0
  }'
```

→ `201 Created`. La réponse contient la session enrichie avec l'exercice dans `exercises[]`.

---

## 8. Sessions filtrées (AND) <a id="8-sessions-filtre"></a>

```bash
curl 'http://localhost:8080/api/v1/training-sessions?filter=type:eq:STRENGTH,durationSeconds:gte:1800&filterOp=AND&sort=startedAt,desc'
```

→ `200 OK`. La réponse contient `content`, `page`, `size`, `totalElements`, `totalPages` et les `_links` de navigation.

---

## 9. Erreur de validation 400 (RFC 7807) <a id="9-erreur-validation"></a>

```bash
curl -X POST http://localhost:8080/api/v1/programs \
  -H 'Content-Type: application/json' \
  -d '{"name":"","startDate":null,"endDate":null}'
```

→ `400 Bad Request` — `Content-Type: application/problem+json`

```json
{
  "type": "https://fittracker.dev/problems/validation",
  "title": "Donnees invalides",
  "status": 400,
  "detail": "La requete contient des champs invalides",
  "instance": "/api/v1/programs",
  "timestamp": "2026-06-04T18:01:00Z",
  "traceId": "65f6a1...",
  "errors": [
    { "field": "name",      "code": "NotBlank", "message": "must not be blank", "rejectedValue": "" },
    { "field": "startDate", "code": "NotNull",  "message": "must not be null",  "rejectedValue": "null" },
    { "field": "endDate",   "code": "NotNull",  "message": "must not be null",  "rejectedValue": "null" }
  ]
}
```

---

## 10. Erreur métier 422 (RFC 7807) <a id="10-erreur-metier"></a>

```bash
curl -X POST http://localhost:8080/api/v1/programs \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Prep semi-marathon",
    "startDate":"2026-09-15",
    "endDate":"2026-06-15"
  }'
```

→ `422 Unprocessable Entity` — `Content-Type: application/problem+json`

```json
{
  "type": "https://fittracker.dev/problems/business-rule",
  "title": "Regle metier violee",
  "status": 422,
  "detail": "endDate doit etre apres startDate",
  "instance": "/api/v1/programs",
  "timestamp": "2026-06-04T18:02:00Z",
  "traceId": "65f6a1..."
}
```

---

## 11. Notifications par curseur <a id="11-notifications-cursor"></a>

```bash
# Premiere page
curl 'http://localhost:8080/api/v1/notifications?size=2'
```

→ `200 OK`

```json
{
  "content": [
    { "id": "...", "type": "NEW_PR",      "payload": {"exercise":"squat","weight":110.0}, "createdAt": "...", "_links": { "mark-read": {"href":"..."} } },
    { "id": "...", "type": "ACHIEVEMENT", "payload": {"badge":"first-week-streak"},      "createdAt": "...", "_links": { "mark-read": {"href":"..."} } }
  ],
  "nextCursor": "ZDBl...",
  "size": 2,
  "_links": {
    "self": { "href": ".../api/v1/notifications?size=2" },
    "next": { "href": ".../api/v1/notifications?cursor=ZDBl...&size=2" }
  }
}
```

Marquer comme lue :

```bash
curl -X PATCH http://localhost:8080/api/v1/notifications/<id>/read
```

→ `200 OK` avec `readAt` rempli.

---

## 12. Versioning par négociation de contenu <a id="12-versioning"></a>

```bash
# v1 (par défaut) - réponse + headers de dépréciation
curl -i http://localhost:8080/api/v1/version-demo/widgets
```

```
HTTP/1.1 200
Deprecation: true
Sunset: Wed, 16 Jun 2027 00:00:00 GMT
Link: </api/v1/version-demo/widgets>; rel="successor-version"; type="application/vnd.fittracker.v2+json"
Content-Type: application/json

[{"id":"w1","name":"Widget A"},{"id":"w2","name":"Widget B"}]
```

```bash
# v2 via Accept
curl -i -H 'Accept: application/vnd.fittracker.v2+json' \
  http://localhost:8080/api/v1/version-demo/widgets
```

```
HTTP/1.1 200
Content-Type: application/vnd.fittracker.v2+json

{
  "version": "v2",
  "items": [
    {"id":"w1","name":"Widget A","tags":["alpha","beta"]},
    {"id":"w2","name":"Widget B","tags":["beta"]}
  ],
  "count": 2
}
```

**Stratégies de versioning utilisées par FitTracker** :

- **Préfixe URI** (principal) : `/api/v1/...`. Simple, lisible, cacheable.
- **Négociation de contenu** (secondaire) : `Accept: application/vnd.fittracker.v2+json`. Permet d'évoluer le payload sans changer l'URI.
- **Dépréciation** : tout endpoint legacy renvoie `Deprecation: true` + `Sunset: <date>` + `Link: …; rel="successor-version"` (RFC 8594 / draft-ietf-deprecation-header).
