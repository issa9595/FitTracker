# FitTracker — Suivi & checklist

> Document de suivi de l'avancement par rapport au brief contractuel (`/Users/madayev/Downloads/fittracker_brief.md`).
> Rendu : **16/06/2026**. 1 phase = 1 livrable.
> Légende : ✅ fait · 🔄 en cours · ⬜ à faire · ⏭️ phase ultérieure

**Dernière mise à jour : 2026-06-16**

---

## Règles permanentes (§0) — à vérifier à CHAQUE commit/phase

- [ ] Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `ci:`), 1 commit = 1 changement atomique
- [ ] Ne jamais merger une phase tant que la CI n'est pas verte
- [ ] `README.md` + `docs/` mis à jour à chaque phase
- [ ] Aucun secret en clair (que des `.env.example`)
- [ ] Pas de `System.out.println` (SLF4J + Logback, JSON en prod)
- [ ] Pas de code mort
- [ ] Tests à chaque phase (≥ 1 unitaire + 1 intégration par fonctionnalité)
- [ ] OpenAPI auto-généré maintenu à jour

---

## Vue d'ensemble des phases

| Phase | Livrable | Statut | PR / Branche |
|---|---|---|---|
| 1 | Bootstrap, Docker, CI | ✅ Mergée | PR #1 |
| 2 | Twelve-Factor, Compose, Nginx, logs JSON | ✅ Mergée | PR #2 |
| 3 | REST, HATEOAS, RFC 7807, pagination, versioning | ✅ Mergée | PR #3 (`06eac24`) |
| 4 | Persistence ORM, relations, CRUD, RGPD | ✅ Mergée | PR #4 |
| 5 | WebSockets temps réel, notifications | ✅ Mergée | PR #5 |
| 6 | Sécurité durcie : Spring Security resource server, OWASP | ✅ Mergée | PR #6 |
| 7 | Tests, couverture JaCoCo, CI/CD enrichie (release GHCR) | ✅ Mergée | PR #7 |

---

## ✅ Phase 1 — Bootstrap projet, Docker, CI (MERGÉE)

- [x] `pom.xml` Spring Boot 3.3 (web, validation, actuator)
- [x] `Dockerfile` multi-stage (builder Maven + runtime JRE-alpine non-root, HEALTHCHECK)
- [x] `.dockerignore` + `.gitignore`
- [x] Simulation push registry (`Makefile` / `scripts/build-and-push.sh`)
- [x] CI GitHub Actions (`lint` → `build` → `docker-build`, cache Maven, Java 21)
- [x] `README.md` quick start + badge CI

---

## ✅ Phase 2 — Twelve-Factor, Compose, Nginx, logs (MERGÉE)

- [x] Config externalisée (`${VAR:default}`) + `.env.example`
- [x] Logs dev (texte) / prod (JSON LogstashEncoder, traceId, rotation)
- [x] `docker-compose.yml` (app, db, redis, nginx) + healthchecks + réseau dédié
- [x] `docker-compose.override.yml` (dev)
- [x] `nginx.conf` (upstream, headers sécurité, `/ws/**` upgrade, gzip)
- [x] `docs/twelve-factor.md` + `docs/security.md` (vue d'ensemble)

---

## ✅ Phase 3 — API REST, HATEOAS, RFC 7807 (MERGÉE)

- [x] Endpoints v1 (auth, users/me, training-sessions, programs, follows, exercises, notifications)
- [x] Conventions URI (pluriel, ≤ 2 niveaux, pas de verbe)
- [x] HATEOAS (`_links` self/related/next/prev)
- [x] Erreurs RFC 7807 (`@RestControllerAdvice`, `problem+json`, 400/401/403/404/409/422/500)
- [x] Pagination offset + cursor
- [x] Filtrage dynamique (DSL `filter=...`, opérateurs eq/neq/gt/gte/lt/lte/in/like, AND/OR)
- [x] Versioning URI + négociation de contenu + headers de dépréciation
- [x] OpenAPI/Swagger UI annoté
- [x] `docs/api-examples.md` (≥ 10 paires requête/réponse)
- [x] Tests MockMvc par controller (nominal + erreur)

> ⚠️ Phase 3 utilise un `InMemoryRepository` (POJOs en mémoire) — remplacé en phase 4 par JPA.

---

## 🔄 Phase 4 — Persistence ORM, relations, CRUD, RGPD (EN COURS)

**Branche : `feature/phase-4-persistence`**

### ⚠️ Bloqueur environnement local
- [ ] **JDK 21 installé** (`java -version` KO actuellement → `brew install --cask temurin@21`)
- [ ] **Docker daemon démarré** (requis pour Postgres local + Testcontainers)

### Dépendances & config
- [~] `pom.xml` : +JPA, Flyway, Postgres, Testcontainers, MapStruct *(édité, pas encore commit/vérifié)*
- [ ] `application.yml` : `spring.jpa` (ddl-auto=validate, open-in-view=false) + `spring.flyway`
- [ ] `src/test/resources/application-test.yml`

### Migrations Flyway (`src/main/resources/db/migration/`)
- [ ] `V1__init.sql` : 8 tables + FK + index (colonnes filtrées/triées)
- [ ] `V2__seed_exercises.sql` : référentiel (running, MMA, lutte, muscu) — UUID déterministes
- [ ] `V3__seed_deleted_user.sql` : user sentinelle pour anonymisation RGPD

### Entités JPA
- [ ] `@OneToOne` User ↔ Profile avec `@MapsId` (PK partagée)
- [ ] `@OneToMany`/`@ManyToOne` LAZY + `@JsonIgnore` côté inverse (pas de cycles)
- [ ] `@ManyToMany` avec entités d'association explicites (`SessionExercise`, `Follow`)
- [ ] `@Version` (optimistic locking) sur entités modifiables
- [ ] Soft-delete `deletedAt` via `@SQLDelete` + `@SQLRestriction`
- [ ] Auditing `@CreatedDate`/`@LastModifiedDate` + `@EnableJpaAuditing`
- [ ] `Notification.payload` en JSONB (`@JdbcTypeCode(SqlTypes.JSON)`)

### Repositories
- [ ] Tous en `JpaRepository`
- [ ] `JpaSpecificationExecutor` pour les entités filtrables (TrainingSession)
- [ ] Suppression de `InMemoryRepository` + adaptation des services

### CRUD + règles métier
- [ ] Création séance vérifie l'existence des exercices référencés
- [ ] MAJ/suppression vérifie le propriétaire
- [ ] `@Transactional` sur les services multi-entités

### Endpoints RGPD
- [ ] `GET /api/v1/users/me/export` (JSON exhaustif : sessions, programmes, follows, notifs, profil)
- [ ] `DELETE /api/v1/users/me` → pipeline : anonymise sessions (→ user "deleted"), purge follows/profil/notifs

### Tests
- [ ] Tests unitaires services (Mockito + AssertJ)
- [ ] Tests d'intégration **Testcontainers Postgres** : 1-1, 1-N, M-N avec attrs, M-N self-ref
- [ ] Test d'intégration pipeline RGPD (export + anonymisation)

### Documentation
- [ ] `docs/architecture.md` avec **ERD Mermaid** + diagramme containers
- [ ] `README.md` section persistance

### Vérifications de fin de phase (brief §4)
- [ ] `mvn verify` vert en local
- [ ] Toutes les relations démontrées par un test d'intégration
- [ ] Export RGPD exhaustif vérifié
- [ ] CI verte sur la PR
- [ ] **STOP** → validation utilisateur avant phase 5

---

## ⏭️ Phase 5 — WebSockets temps réel, notifications

- [ ] Config WebSocket STOMP (`/ws`, broker dev/Redis, destinations user + feed)
- [ ] Auth JWT à l'ouverture (`ChannelInterceptor`, Principal)
- [ ] Heartbeat `{10000, 10000}` + doc reconnexion backoff
- [ ] Service notifications (`ApplicationEventPublisher` → `NotificationListener` → BDD + `SimpMessagingTemplate`)
- [ ] Client UI minimal (`notifications.html`, `@stomp/stompjs`, login + liste + marquer lu)
- [ ] Test d'intégration `WebSocketStompClient` (flux complet)
- [ ] `docs/websockets.md`

---

## ⏭️ Phase 6 — Sécurité durcie : JWT, OAuth2, TLS

- [ ] `JwtService` (RS256 préféré), access 15 min + refresh 7 j en BDD (table `refresh_tokens`)
- [ ] `POST /api/v1/auth/refresh` + `POST /api/v1/auth/logout`
- [ ] Spring Security Resource Server (JWT internes)
- [ ] OAuth2 Authorization Code (Google/GitHub) + endpoints oauth2
- [ ] TLS Nginx (certif self-signed) + `scripts/rotate-certs.sh` + mTLS documenté
- [ ] Hardening : CSRF off documenté, CORS whitelist, rate limiting Bucket4j+Redis, headers sécurité
- [ ] OWASP Top 10 checklist dans `docs/security.md`
- [ ] Tests : 401 sans token, 403 ressource d'autrui, OAuth2 démo, TLS via Nginx

---

## ✅ Phase 7 — Tests, couverture, CI/CD enrichie (version allégée)

**Branche : `feature/phase-7-tests-cicd`**

- [x] Couverture JaCoCo double agent (Surefire + Failsafe) + merge, rapport en artefact CI
- [x] Règle de couverture vérifiée à `verify` : **≥ 80 %** par `*Service` (réel 98,5 %), **≥ 70 %** global (réel 79,2 %)
- [x] Tests unitaires de services (Mockito + AssertJ, nommage `should_<expected>_when_<context>`)
- [x] Tests d'intégration Testcontainers Postgres + bout-en-bout WebSocket (existants, phases 4/5)
- [x] `release.yml` (tag `v*`, push GHCR, release notes auto)
- [x] `docs/testing.md` (pyramide, double agent JaCoCo, exclusions)
- [x] Spotless + Checkstyle en CI (job `lint`, déjà présent)
- [~] Badge couverture README → mention textuelle + lien artefact (pas de service externe)
- [ ] ~~(Optionnel) Cucumber JVM~~ — ignoré (version allégée, cf. `docs/testing.md` §6)

---

## Documentation finale attendue (§5) — état

- [x] `README.md`
- [x] `docs/twelve-factor.md`
- [x] `docs/security.md` (OWASP & RGPD — complété phase 6)
- [x] `docs/api-examples.md`
- [x] `docs/architecture.md` (ERD) — phase 4
- [x] `docs/websockets.md` — phase 5
- [x] `docs/testing.md` — phase 7
