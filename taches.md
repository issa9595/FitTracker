# FitTracker — Suivi & checklist

> Document de suivi de l'avancement par rapport au brief contractuel.
> Rendu : **16/06/2026**. 1 phase = 1 livrable.
> Légende : ✅ fait · 🔄 en cours · ⬜ à faire · 🟡 documenté en évolution

**Dernière mise à jour : 2026-06-16 — projet complet (7/7 livrables mergés).**

---

## Règles permanentes (§0) — respectées sur tout le projet

- [x] Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `ci:`), 1 commit = 1 changement atomique
- [x] Ne jamais merger une phase tant que la CI n'est pas verte
- [x] `README.md` + `docs/` mis à jour à chaque phase
- [x] Aucun secret en clair (que des `.env.example`)
- [x] Pas de `System.out.println` (SLF4J + Logback, JSON en prod)
- [x] Pas de code mort
- [x] Tests à chaque phase (≥ 1 unitaire + 1 intégration par fonctionnalité)
- [x] OpenAPI auto-généré maintenu à jour

---

## Vue d'ensemble des phases

| Phase | Livrable | Statut | PR |
|---|---|---|---|
| 1 | Bootstrap, Docker, CI | ✅ Mergée | PR #1 |
| 2 | Twelve-Factor, Compose, Nginx, logs JSON | ✅ Mergée | PR #2 |
| 3 | REST, HATEOAS, RFC 7807, pagination, versioning | ✅ Mergée | PR #3 |
| 4 | Persistence ORM, relations, CRUD, RGPD | ✅ Mergée | PR #4 |
| 5 | WebSockets temps réel, notifications | ✅ Mergée | PR #5 |
| 6 | Sécurité durcie (Spring Security, OWASP) | ✅ Mergée | PR #6 |
| 7 | Tests, couverture JaCoCo, CI/CD enrichie | ✅ Mergée | PR #7 |

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

---

## ✅ Phase 4 — Persistence ORM, relations, CRUD, RGPD (MERGÉE)

- [x] Migrations Flyway V1 (schéma) / V2 (seed exercices) / V3 (user sentinelle RGPD)
- [x] `@OneToOne` User ↔ Profile avec `@MapsId` (PK partagée)
- [x] `@OneToMany`/`@ManyToOne` LAZY (User → Sessions/Programs/Notifications)
- [x] M-N avec attributs (`SessionExercise`) + M-N self-référençant (`Follow`), clés composites
- [x] `@Version` (optimistic locking), soft-delete `@SQLDelete` + `@SQLRestriction`
- [x] Auditing `@CreatedDate`/`@LastModifiedDate`, `Notification.payload` en JSONB
- [x] Repositories `JpaRepository` (+ `JpaSpecificationExecutor` sur TrainingSession), suppression `InMemoryRepository`
- [x] CRUD + règles métier (existence exercices, ownership, `@Transactional`)
- [x] RGPD : `GET /api/v1/users/me/export` + `DELETE /api/v1/users/me` (anonymisation)
- [x] Tests Testcontainers : 1-1, 1-N, M-N avec attrs, M-N self-ref + pipeline RGPD
- [x] `docs/architecture.md` (ERD Mermaid + diagramme containers)
- [x] `mvn verify` vert, CI verte, mergée

---

## ✅ Phase 5 — WebSockets temps réel, notifications (MERGÉE)

- [x] Config WebSocket STOMP (`/ws`, SimpleBroker `/topic`, heartbeats {10000,10000})
- [x] Auth JWT au CONNECT (`JwtChannelInterceptor`, `Principal`)
- [x] Reconnexion backoff documentée
- [x] Service notifications (`ApplicationEventPublisher` → `NotificationListener` → BDD + `SimpMessagingTemplate`)
- [x] Événements métier : séance d'un user suivi (FRIEND_SESSION_COMPLETED), nouveau PR (NEW_PR)
- [x] UI minimale `notifications.html` (login + liste + marquer lu)
- [x] Test d'intégration `WebSocketStompClient` (flux complet)
- [x] `docs/websockets.md`
- 🟡 Broker Redis multi-instance : documenté en évolution (mono-instance suffit pour la démo)

---

## ✅ Phase 6 — Sécurité durcie (version ciblée) (MERGÉE)

- [x] Spring Security Resource Server (JWT HS256, même secret que `JwtService`)
- [x] BCrypt cost 12 (login vérifie le mot de passe, 401 générique anti-énumération)
- [x] CORS whitelist + headers (HSTS, frameOptions deny, nosniff, referrer-policy)
- [x] Rate limiting Bucket4j (60 req/min/IP sur `/auth/**`), 429 `problem+json`
- [x] CSRF désactivé documenté, erreurs 401/403 en RFC 7807
- [x] OWASP Top 10 checklist dans `docs/security.md`
- [x] Tests : 401 sans token, 403 ressource d'autrui, rate limiter
- 🟡 Évolutions documentées (non implémentées) : refresh tokens + `/auth/refresh` `/auth/logout`, OAuth2 Authorization Code (Google/GitHub), RS256, TLS Nginx + rotation, Bucket4j-Redis distribué

---

## ✅ Phase 7 — Tests, couverture, CI/CD enrichie (MERGÉE)

- [x] Couverture JaCoCo double agent (Surefire + Failsafe) + merge, rapport en artefact CI
- [x] Règle de couverture vérifiée à `verify` : **≥ 80 %** par `*Service` (réel 98,5 %), **≥ 70 %** global (réel 79,2 %)
- [x] Tests unitaires de services (Mockito + AssertJ, nommage `should_<expected>_when_<context>`)
- [x] Tests d'intégration Testcontainers Postgres + bout-en-bout WebSocket
- [x] `release.yml` (tag `v*`, push GHCR, release notes auto)
- [x] `docs/testing.md` (pyramide, double agent JaCoCo, exclusions)
- [x] Spotless + Checkstyle en CI (job `lint`)
- [x] Couverture README : mention textuelle + lien artefact CI
- 🟡 Cucumber JVM : ignoré (version allégée, cf. `docs/testing.md` §6)

---

## Documentation finale attendue (§5) — état

- [x] `README.md`
- [x] `docs/twelve-factor.md`
- [x] `docs/security.md` (OWASP & RGPD — complété phase 6)
- [x] `docs/api-examples.md`
- [x] `docs/architecture.md` (ERD) — phase 4
- [x] `docs/websockets.md` — phase 5
- [x] `docs/testing.md` — phase 7
