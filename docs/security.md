# Sécurité FitTracker — vue d'ensemble OWASP & RGPD

Ce document est un **squelette** ouvert dès la Phase 2. Il sera enrichi au fil des phases (4 RGPD, 6 hardening complet). Les sections marquées 🟡 ne sont pas encore implémentées, elles documentent l'intention.

---

## OWASP Top 10 — mitigations prévues

| ID | Risque | Mitigation FitTracker | Phase |
|---|---|---|---|
| A01 | Broken Access Control | Spring Security + autorisation per-resource via `@PreAuthorize`, tests d'intégration cross-user (un user ne peut accéder aux ressources d'un autre) | 6 |
| A02 | Cryptographic Failures | JWT RS256 (rotation de clés), BCrypt(12) pour les mots de passe, TLS partout via Nginx | 6 |
| A03 | Injection | JPA paramétré (jamais de concaténation SQL), validation `@Valid` sur tous les DTO entrants, échappement HTML par défaut | 3-4 |
| A04 | Insecure Design | Modélisation domaine via DDD léger, pas d'exposition d'IDs séquentiels prédictibles (UUID v7 imposés au §2 du brief) | 4 |
| A05 | Security Misconfiguration | Headers de sécurité par Nginx (HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, CSP) ; profils Spring `dev`/`prod` stricts ; actuator restreint en prod (`health,info`) | 2 ✅ |
| A06 | Vulnerable Components | Dependabot ou Renovate (à activer), `mvn versions:display-dependency-updates` dans la CI mensuelle | 7 |
| A07 | Authentication Failures | Lockout après 5 échecs (compteur Redis), MFA mentionnée en évolution, refresh tokens hashés en BDD et révocables | 6 |
| A08 | Software & Data Integrity | Image Docker signée + SHA pin sur les images de base, `--platform linux/amd64` explicite dans la CI | 7 |
| A09 | Logging Failures | Logs JSON structurés avec `traceId` (Micrometer Tracing), événements d'auth tracés (login, logout, refresh, failed) | 2 ✅ / 6 |
| A10 | SSRF | Pas d'appel HTTP sortant déclenché par l'utilisateur en l'état ; si OAuth2 externe en phase 6, validation stricte des URLs de redirect |

### Headers de sécurité actifs en Phase 2

Définis dans [`docker/nginx/conf.d/fittracker.conf`](../docker/nginx/conf.d/fittracker.conf), s'appliquent à **toutes** les réponses :

- `Strict-Transport-Security: max-age=31536000; includeSubDomains` — actif dès l'ouverture du :443 (phase 6)
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-XSS-Protection: 0` (désactivé conformément aux recommandations OWASP 2024)
- `Content-Security-Policy` : `default-src 'self'`, `connect-src 'self' ws: wss:` pour autoriser les WebSockets phase 5, `frame-ancestors 'none'`, `base-uri 'self'`, `form-action 'self'`

### CSRF — choix documenté

CSRF est **désactivé** sur les routes `/api/v1/**` (API stateless, JWT) — décision standard pour les API REST consommées par des clients non-cookie. Sera explicité dans la `SecurityConfig` de la phase 6.

---

## RGPD

| Exigence | Application FitTracker | Phase |
|---|---|---|
| Minimisation | Profile contient le strict nécessaire (height/weight/goal/bio), pas de date de naissance ni localisation | 4 |
| Droit à la portabilité | `GET /api/v1/users/me/export` → JSON exhaustif des données utilisateur | 4 |
| Droit à l'effacement | `DELETE /api/v1/users/me` → pipeline d'anonymisation : `user_id` remplacé par utilisateur "deleted" sur les sessions (préserve les agrégats stats), suppression cascade des `follows`, `profile`, `notifications` | 4 |
| Soft-delete | Colonne `deleted_at` sur `users` (RGPD) via `@SQLDelete`/`@Where` Hibernate | 4 |
| Limitation de conservation | Refresh tokens : TTL 7 jours en BDD + GC automatique. Logs applicatifs : 7 jours d'historique si rotation fichier active | 2 ✅ / 6 |
| Sécurité du stockage | Mots de passe BCrypt(12), JWT RS256, TLS partout pour les flux | 6 |
| Journalisation des accès | Logs JSON avec `traceId` corrélant requête + actions BDD ; événements d'auth dédiés en phase 6 | 2 ✅ |
| Notification de violation | Procédure interne à documenter (non-applicable à un projet école, mais mentionner) | — |

---

## Chiffrement & secrets

- Secrets **jamais** versionnés. `.env` ignoré par git, `.env.example` documenté.
- En production cible :
  - Mots de passe DB injectés via mécanisme de secrets (Docker secrets, Vault, AWS Secrets Manager…).
  - Clés RSA JWT montées en read-only via `secrets:` dans docker compose (phase 6).
  - Certificats TLS rotatifs (`scripts/rotate-certs.sh` en phase 6).
- mTLS entre app et Redis ou broker mentionné comme évolution, exemple de config documentée même si non activée.

---

## Authentification & autorisation (phase 6)

Détails complets en phase 6. Vue d'ensemble :

- Inscription / login : `POST /api/v1/auth/register` + `POST /api/v1/auth/login` → access token (15 min) + refresh token (7 j, hashé en BDD).
- Refresh : `POST /api/v1/auth/refresh`.
- Logout : `POST /api/v1/auth/logout` (révoque le refresh).
- OAuth2 Authorization Code via Google/GitHub : `GET /api/v1/auth/oauth2/{provider}` + callback `/api/v1/auth/oauth2/callback/{provider}`.
- Spring Security configuré en Resource Server pour les JWT internes (`spring-boot-starter-oauth2-resource-server`).
- Rate limiting : Bucket4j backé par Redis, 60 req/min/IP sur `/auth/*`.

---

## Surveillance & audit

- Logs JSON corrélés (`traceId`) → ingestion possible dans Loki, ELK, Datadog…
- Métriques Micrometer exposées via `/actuator/prometheus` (à activer en phase 7).
- Événements d'auth (login OK, failed, refresh, logout) tracés à `INFO` avec `event_type` dédié.
- Healthcheck Docker → l'orchestrateur peut redémarrer un conteneur unhealthy.

---

## Checklist par phase

- [x] **Phase 2** : Headers Nginx, profils prod/dev séparés, secrets externalisés (env vars), logs JSON, base sécurité documentée.
- [ ] **Phase 4** : RGPD complet (export, anonymisation, soft-delete), audit `@CreatedDate`/`@LastModifiedDate`.
- [ ] **Phase 6** : Auth complète, TLS, OWASP Top 10 testée par tests d'intégration, MFA mentionnée, mTLS documenté.
- [ ] **Phase 7** : SCA via Dependabot ou Renovate, image scan (Trivy ou similaire).
