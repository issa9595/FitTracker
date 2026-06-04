# FitTracker

[![CI](https://github.com/issa9595/FitTracker/actions/workflows/ci.yml/badge.svg)](https://github.com/issa9595/FitTracker/actions/workflows/ci.yml)

Back-end Java de suivi d'entraînement sportif — projet de fin d'année (rendu **16/06/2026**).

> **État** : Phase 1 — bootstrap projet, Docker, CI. L'application expose un endpoint `/actuator/health` minimal.

## Stack

| Couche | Choix |
|---|---|
| Langage | Java 21 (LTS) |
| Framework | Spring Boot 3.3 |
| Build | Maven 3.9 (via wrapper `./mvnw`) |
| Conteneur | Docker (multi-stage), Alpine JRE |
| CI | GitHub Actions |
| Qualité | Spotless (google-java-format) + Checkstyle |

Stack cible complète (PostgreSQL 16, Redis 7, Flyway, JJWT, Testcontainers, Nginx, etc.) : voir `fittracker_brief.md`.

## Pré-requis

- **Java 21** (Temurin recommandé)
- **Docker** + **Docker Compose v2** (pour la phase 2+)
- **Git**
- Maven n'est PAS requis : le wrapper `./mvnw` télécharge la bonne version au premier appel

## Quick start

### En local (sans Docker)

```bash
./mvnw spring-boot:run
# puis dans un autre terminal :
curl http://localhost:8080/actuator/health
# -> {"status":"UP"}
```

### Build + lancement Docker

```bash
# Build de l'image (tag local fittracker:<sha-court>)
./scripts/build-and-push.sh

# Lancement
docker run --rm -p 8080:8080 fittracker:$(git rev-parse --short=12 HEAD)

# Health check
curl http://localhost:8080/actuator/health
```

### Cibles Make utiles

```bash
make build          # mvn package (skip tests)
make verify         # mvn verify (lint + tests)
make lint           # spotless:check + checkstyle:check
make format         # spotless:apply (reformate)
make docker-build   # build image Docker via scripts/build-and-push.sh
make docker-run     # build + run sur 8080
```

## Structure du repo

```
fittracker/
├── .github/workflows/ci.yml      # pipeline lint → build → docker-build
├── .mvn/wrapper/                 # Maven Wrapper
├── scripts/build-and-push.sh     # build + tag image, push optionnel
├── src/
│   ├── main/
│   │   ├── java/com/fittracker/  # code applicatif
│   │   └── resources/
│   │       └── application.yml   # config par defaut, placeholders ${VAR:default}
│   └── test/java/com/fittracker/ # tests
├── Dockerfile                    # multi-stage, JRE Alpine, user non-root
├── Makefile                      # raccourcis
├── checkstyle.xml                # rules Checkstyle
├── pom.xml                       # Spring Boot 3.3, plugins Spotless + Checkstyle
├── .env.example                  # variables d'env (a copier en .env, non commit)
├── .gitignore                    # Java/Maven/IDE/macOS/secrets
└── .dockerignore                 # exclusions build context Docker
```

## CI

Pipeline GitHub Actions déclenché sur `push` vers `main` et sur chaque pull request.

| Job | Contenu |
|---|---|
| `lint` | `./mvnw spotless:check` + `./mvnw checkstyle:check` |
| `build` | `./mvnw verify -DskipITs` (Java 21), artefact `fittracker.jar` |
| `docker-build` | Build image Docker (sans push), smoke test `curl /actuator/health` |

Cache Maven activé (`actions/setup-java` avec `cache: maven`) + cache GHA pour les couches Docker.

## Conventions

- **Commits** : [Conventional Commits](https://www.conventionalcommits.org/) — `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `ci:`, `build:`.
- **Code** : formaté par Spotless (google-java-format), conformité Checkstyle vérifiée en CI.
- **Logs** : SLF4J uniquement, jamais `System.out.println` (interdit par Checkstyle).
- **Secrets** : aucun secret en clair, uniquement des `.env.example`.

## Roadmap

| Phase | Contenu | État |
|---|---|---|
| 1 | Bootstrap projet, Docker, CI | en cours |
| 2 | Twelve-Factor, Docker Compose, Nginx, logs structurés | à faire |
| 3 | API REST, HATEOAS, RFC 7807, pagination, versioning | à faire |
| 4 | Persistence ORM, relations, CRUD, RGPD | à faire |
| 5 | WebSockets temps réel, notifications | à faire |
| 6 | Sécurité durcie : JWT, OAuth2, TLS | à faire |
| 7 | Tests complets, qualité, CI/CD enrichie | à faire |

Détails dans `fittracker_brief.md` (contrat du projet).
