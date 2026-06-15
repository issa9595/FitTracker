# FitTracker

[![CI](https://github.com/issa9595/FitTracker/actions/workflows/ci.yml/badge.svg)](https://github.com/issa9595/FitTracker/actions/workflows/ci.yml)

Back-end Java de suivi d'entraînement sportif — projet de fin d'année (rendu **16/06/2026**).

> **État** : Phase 4 — Persistence JPA/Hibernate sur PostgreSQL (Flyway, relations 1-1/1-N/M-N, soft-delete, audit, export & anonymisation RGPD).

## Stack

| Couche | Choix |
|---|---|
| Langage | Java 21 (LTS) |
| Framework | Spring Boot 3.3 |
| Build | Maven 3.9 (via wrapper `./mvnw`) |
| Conteneur | Docker (multi-stage, JLink JRE) + Docker Compose v2 |
| BDD | PostgreSQL 16 alpine (compose) |
| Cache / pub-sub | Redis 7 alpine (compose) |
| Reverse-proxy | Nginx 1.27 alpine (compose) |
| Observabilité | Micrometer Tracing + Logback JSON (LogstashEncoder) |
| API | Spring HATEOAS + springdoc-openapi (Swagger UI) |
| CI | GitHub Actions |
| Qualité | Spotless (google-java-format) + Checkstyle |

Stack cible complète (Flyway, JJWT, OAuth2, Testcontainers, etc.) : voir `fittracker_brief.md`.

## Pré-requis

- **Java 21** (Temurin recommandé) — uniquement pour le run/dev local sans Docker
- **Docker** + **Docker Compose v2**
- **Git**
- Maven n'est PAS requis : le wrapper `./mvnw` télécharge la bonne version au premier appel

## Quick start

### Stack complète via Docker Compose (recommandé)

```bash
cp .env.example .env                # adapter les valeurs si besoin
docker compose up -d --build        # construit l'image puis lance app + db + redis + nginx
docker compose ps                   # tous les services doivent être 'healthy'

# Accès via Nginx (port 80 par défaut)
curl http://localhost/actuator/health
# -> {"status":"UP"}

# Vérifier que les logs prod sont du JSON valide
docker compose logs app --no-log-prefix | head -5 | jq .
```

L'`docker-compose.override.yml` est appliqué automatiquement et :
- force `SPRING_PROFILES_ACTIVE=dev` (logs console texte coloré)
- expose `8080` (app), `5432` (db), `6379` (redis) sur l'hôte pour debug
- monte `target/fittracker.jar` dans le conteneur pour itération rapide

### Lancer le profil prod (logs JSON)

```bash
SPRING_PROFILES_ACTIVE=prod docker compose -f docker-compose.yml up -d --build
```

### En local sans Docker (app seule)

```bash
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
```

### Build d'image et push registry

```bash
./scripts/build-and-push.sh           # build + tag local + tag remote (pas de push)
./scripts/build-and-push.sh --push    # idem + push vers ${REGISTRY}
```

### Cibles Make

```bash
make build          # mvn package (skip tests)
make verify         # mvn verify (lint + tests)
make lint           # spotless:check + checkstyle:check
make format         # spotless:apply (reformate)
make docker-build   # build image Docker
make docker-run     # build + run image sur 8080
```

## Structure du repo

```
fittracker/
├── .github/workflows/ci.yml         # pipeline lint → build → docker-build
├── .mvn/wrapper/                    # Maven Wrapper
├── docker/
│   └── nginx/                       # nginx.conf + conf.d/fittracker.conf
├── docs/
│   ├── twelve-factor.md             # 12-Factor : où chaque facteur est appliqué
│   └── security.md                  # OWASP & RGPD, vue d'ensemble
├── scripts/build-and-push.sh        # build + tag image, push optionnel
├── src/
│   ├── main/
│   │   ├── java/com/fittracker/     # code applicatif
│   │   └── resources/
│   │       ├── application.yml      # config dev par défaut, placeholders ${VAR}
│   │       ├── application-prod.yml # surcharges prod via env vars (no defaults)
│   │       └── logback-spring.xml   # dev=texte coloré, prod=JSON
│   └── test/java/com/fittracker/
├── docker-compose.yml               # 4 services prod : app + db + redis + nginx
├── docker-compose.override.yml      # surcharges dev
├── Dockerfile                       # multi-stage, JRE jlink, user non-root
├── Makefile                         # raccourcis
├── checkstyle.xml                   # rules Checkstyle
├── pom.xml                          # Spring Boot 3.3 + plugins qualité
├── .env.example                     # toutes les variables d'env documentées
├── .gitignore / .dockerignore       # Java/Maven/IDE/macOS/secrets
└── README.md
```

## CI

Pipeline GitHub Actions déclenché sur `push` vers `main` et sur chaque pull request.

| Job | Contenu |
|---|---|
| `lint` | `./mvnw spotless:check` + `./mvnw checkstyle:check` |
| `build` | `./mvnw verify` (Java 21) — tests unitaires + intégration Testcontainers Postgres, artefact `fittracker.jar` |
| `docker-build` | Build image Docker (sans push), smoke test `curl /actuator/health`, fail si > 250 Mo |
| `compose-validate` | Démarre la stack complète (app+db+redis+nginx) et valide santé, headers, logs JSON |

Cache Maven activé (`actions/setup-java` avec `cache: maven`) + cache GHA pour les couches Docker.

## Conventions

- **Commits** : [Conventional Commits](https://www.conventionalcommits.org/) — `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `ci:`, `build:`.
- **Code** : formaté par Spotless (google-java-format), conformité Checkstyle vérifiée en CI.
- **Logs** : SLF4J uniquement, jamais `System.out.println` (interdit par Checkstyle).
- **Config** : `${VAR:default}` partout, jamais de secret en clair (`.env.example` uniquement).
- **Logs prod** : JSON un-événement-par-ligne, traceId/spanId via Micrometer Tracing.

## API

- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs
- Tous les endpoints sous `/api/v1/...` avec HATEOAS (`_links` self / related / next-prev pour les collections).
- Erreurs sérialisées en `application/problem+json` (RFC 7807) avec extensions `errors[]`, `traceId`.
- Pagination **offset/limit** par défaut (`?page=0&size=20&sort=startedAt,desc`), **cursor** sur les flux à fort volume (`?cursor=...&size=20`).
- Filtrage : `?filter=field:op:value,field2:op:value&filterOp=AND`. Opérateurs : `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `in`, `like`.
- Versioning : préfixe URI (principal) + négociation `Accept: application/vnd.fittracker.v2+json` (secondaire), avec headers `Deprecation`/`Sunset`/`Link` sur les endpoints legacy.

## Documentation

- [`docs/twelve-factor.md`](docs/twelve-factor.md) — Application des 12 facteurs
- [`docs/security.md`](docs/security.md) — OWASP Top 10 & RGPD (vue d'ensemble)
- [`docs/api-examples.md`](docs/api-examples.md) — 12 paires requête/réponse (auth, pagination, filtrage, erreurs RFC 7807, versioning)
- [`docs/architecture.md`](docs/architecture.md) — schéma containers + ERD Mermaid + choix de persistance (phase 4)

## Persistence (phase 4)

Le schéma PostgreSQL est géré par **Flyway** (`src/main/resources/db/migration/`) :

| Migration | Contenu |
|---|---|
| `V1__init.sql` | 8 tables + contraintes FK + index sur les colonnes filtrées/triées |
| `V2__seed_exercises.sql` | référentiel de 12 exercices (UUID déterministes) |
| `V3__seed_deleted_user.sql` | user sentinelle pour l'anonymisation RGPD |

Hibernate tourne en `ddl-auto=validate` (Flyway gère le schéma, jamais `update`). Les 4 types de
relations (1-1 `User`/`Profile` via `@MapsId`, 1-N `User`/`TrainingSession`, M-N avec attributs
`Session`/`Exercise`, M-N self-référençant `Follow`) sont démontrés par des tests d'intégration
Testcontainers (`src/test/java/com/fittracker/persistence/*IT.java`).

Endpoints RGPD : `GET /api/v1/users/me/export` (portabilité) et `DELETE /api/v1/users/me`
(anonymisation des sessions + purge follows/profil/notifications).

```bash
# Lancer la base seule puis l'app (migrations appliquées au démarrage)
docker compose up -d db
./mvnw spring-boot:run
```

## Roadmap

| Phase | Contenu | État |
|---|---|---|
| 1 | Bootstrap projet, Docker, CI | ✅ |
| 2 | Twelve-Factor, Docker Compose, Nginx, logs structurés | ✅ |
| 3 | API REST, HATEOAS, RFC 7807, pagination, versioning | ✅ |
| 4 | Persistence ORM, relations, CRUD, RGPD | en cours |
| 5 | WebSockets temps réel, notifications | à faire |
| 6 | Sécurité durcie : JWT, OAuth2, TLS | à faire |
| 7 | Tests complets, qualité, CI/CD enrichie | à faire |

Détails dans `fittracker_brief.md` (contrat du projet).
